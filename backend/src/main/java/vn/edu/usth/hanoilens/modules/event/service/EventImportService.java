package vn.edu.usth.hanoilens.modules.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.edu.usth.hanoilens.modules.event.adapter.EventSourceClient;
import vn.edu.usth.hanoilens.modules.event.adapter.EventSourceClient.FetchException;
import vn.edu.usth.hanoilens.modules.event.adapter.OutboundUrlPolicy;
import vn.edu.usth.hanoilens.modules.event.config.EventIngestionProperties;
import vn.edu.usth.hanoilens.modules.event.dto.EventAdminDtos.EventSourceRequest;
import vn.edu.usth.hanoilens.modules.event.dto.EventAdminDtos.EventSourceView;
import vn.edu.usth.hanoilens.modules.event.dto.EventAdminDtos.ImportView;
import vn.edu.usth.hanoilens.modules.event.dto.EventAdminDtos.ValidationError;
import vn.edu.usth.hanoilens.modules.event.parser.EventParser;
import vn.edu.usth.hanoilens.modules.event.parser.EventParser.ParseException;
import vn.edu.usth.hanoilens.modules.event.parser.JsonLdEventParser;
import vn.edu.usth.hanoilens.modules.event.parser.ParseSupport;
import vn.edu.usth.hanoilens.modules.event.parser.ParsedEvent;
import vn.edu.usth.hanoilens.modules.event.parser.RssEventParser;
import vn.edu.usth.hanoilens.modules.event.repository.EventIngestionRepository;
import vn.edu.usth.hanoilens.modules.event.repository.EventIngestionRepository.ImportRow;
import vn.edu.usth.hanoilens.modules.event.repository.EventIngestionRepository.Source;
import vn.edu.usth.hanoilens.shared.AdminAuditLog;
import vn.edu.usth.hanoilens.shared.ApiException;
import vn.edu.usth.hanoilens.shared.Ids;

/**
 * Deterministic event ingestion (T28, SECURITY TM-4.4): register an allow-listed source, fetch it with strict
 * limits, extract events with fixed rules, validate, de-duplicate by fingerprint and store every new event as
 * PENDING_REVIEW. Nothing is ever published here; an administrator does that (T29).
 */
@Service
public class EventImportService {

    private static final Logger log = LoggerFactory.getLogger(EventImportService.class);

    static final Set<String> PARSER_TYPES = Set.of("JSON_LD", "RSS", "CSS_SELECTOR");
    private static final Set<String> CONFIG_KEYS = Set.of("defaultVenueName", "startElement", "endElement",
            "venueElement", "linkElement");
    private static final Pattern ELEMENT_NAME = Pattern.compile("^[A-Za-z0-9_-]{1,60}$");
    private static final int MAX_ERRORS = 50;
    private static final int MAX_TITLE = 240;
    private static final int MAX_VENUE = 200;
    private static final int MAX_URL = 2000;
    private static final int MAX_DESCRIPTION = 5000;

    private final EventIngestionRepository repo;
    private final EventSourceClient client;
    private final Map<String, EventParser> parsers;
    private final AdminAuditLog audit;
    private final EventIngestionProperties props;
    private final TaskExecutor executor;
    private final ObjectMapper json;

    EventImportService(EventIngestionRepository repo, EventSourceClient client, JsonLdEventParser jsonLd,
            RssEventParser rss, AdminAuditLog audit, EventIngestionProperties props,
            @Qualifier("eventImportExecutor") TaskExecutor executor, ObjectMapper json) {
        this.repo = repo;
        this.client = client;
        this.parsers = Map.of("JSON_LD", jsonLd, "RSS", rss);
        this.audit = audit;
        this.props = props;
        this.executor = executor;
        this.json = json;
    }

    // ---- admin: sources ---------------------------------------------------------------------------

    @Transactional
    public EventSourceView registerSource(UUID adminId, EventSourceRequest request) {
        if (!PARSER_TYPES.contains(request.parserType())) {
            throw ApiException.invalidField("parserType", "Must be JSON_LD, RSS or CSS_SELECTOR.");
        }
        if ("CSS_SELECTOR".equals(request.parserType())) {
            throw ApiException.invalidField("parserType", "CSS_SELECTOR sources are not supported yet.");
        }
        URI uri;
        try {
            uri = OutboundUrlPolicy.requireAllowedUrl(request.url());
        } catch (IllegalArgumentException ex) {
            throw ApiException.invalidField("url", ex.getMessage());
        }
        Map<String, Object> config = validateConfig(request.parserConfig());
        String url = uri.toString();
        if (repo.sourceUrlExists(url)) {
            throw ApiException.unprocessable("DUPLICATE_SOURCE", "A source with this URL is already registered.");
        }
        UUID id = Ids.newId();
        try {
            repo.insertSource(id, request.name().trim(), url, request.parserType(), config, adminId);
        } catch (DuplicateKeyException ex) {
            throw ApiException.unprocessable("DUPLICATE_SOURCE", "A source with this URL is already registered.");
        }
        audit.record(adminId, "EVENT_SOURCE_CREATE", "EVENT_SOURCE", id,
                Map.of("name", request.name().trim(), "parserType", request.parserType()));
        return new EventSourceView(id, request.name().trim(), url, request.parserType(), config);
    }

    private static Map<String, Object> validateConfig(Map<String, Object> config) {
        if (config == null) {
            return Map.of();
        }
        for (Map.Entry<String, Object> e : config.entrySet()) {
            if (!CONFIG_KEYS.contains(e.getKey())) {
                throw ApiException.invalidField("parserConfig." + e.getKey(), "Unknown parser option.");
            }
            if (!(e.getValue() instanceof String value) || value.isBlank()) {
                throw ApiException.invalidField("parserConfig." + e.getKey(), "Must be a non-empty string.");
            }
            boolean ok = "defaultVenueName".equals(e.getKey()) ? value.length() <= MAX_VENUE
                    : ELEMENT_NAME.matcher(value).matches();
            if (!ok) {
                throw ApiException.invalidField("parserConfig." + e.getKey(), "Value is not allowed.");
            }
        }
        return Map.copyOf(config);
    }

    // ---- admin: imports ---------------------------------------------------------------------------

    /** Creates a PENDING import and runs it in the background once the transaction has committed. */
    @Transactional
    public ImportView startImport(UUID adminId, UUID sourceId) {
        Source source = repo.lockSource(sourceId).filter(Source::active)
                .orElseThrow(() -> ApiException.notFound("Event source not found."));
        if (repo.hasOpenImport(sourceId)) {
            throw ApiException.conflict("IMPORT_IN_PROGRESS", "An import of this source is already running.");
        }
        UUID importId = Ids.newId();
        repo.createImport(importId, sourceId, adminId);
        audit.record(adminId, "EVENT_IMPORT_START", "EVENT_SOURCE", sourceId, Map.of("importId", importId.toString()));
        runAfterCommit(importId);
        return view(repo.findImport(importId).orElseThrow());
    }

    @Transactional(readOnly = true)
    public ImportView getImport(UUID importId) {
        return repo.findImport(importId).map(EventImportService::view)
                .orElseThrow(() -> ApiException.notFound("Event import not found."));
    }

    /** Scheduled crawl (when enabled): one import per active source that has none running. */
    public void crawlAllSources() {
        for (Source source : repo.findActiveSources()) {
            try {
                UUID importId = startScheduled(source);
                if (importId != null) {
                    executor.execute(() -> run(importId));
                }
            } catch (RuntimeException ex) {
                log.warn("Scheduled event import for source {} was not started: {}", source.id(),
                        ex.getClass().getSimpleName());
            }
        }
    }

    /** No lock needed: a rare overlap with an admin-started import only yields duplicates, which are skipped. */
    private UUID startScheduled(Source source) {
        if (repo.hasOpenImport(source.id())) {
            return null;
        }
        UUID importId = Ids.newId();
        repo.createImport(importId, source.id(), source.createdBy());   // attributed to the registering admin
        return importId;
    }

    private void runAfterCommit(UUID importId) {
        if (!props.runAsync()) {
            return;                                                      // tests / manual runs call run() themselves
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                executor.execute(() -> run(importId));
            }
        });
    }

    // ---- the job ----------------------------------------------------------------------------------

    /** Runs one PENDING import to completion. Safe to call twice: the second call finds it RUNNING and returns. */
    public void run(UUID importId) {
        if (!repo.markRunning(importId)) {
            return;
        }
        List<ValidationError> errors = new ArrayList<>();
        int fetched = 0;
        int created = 0;
        int duplicates = 0;
        int rejected = 0;
        try {
            ImportRow job = repo.findImport(importId).orElseThrow();
            Source source = repo.findSource(job.sourceId()).filter(Source::active)
                    .orElseThrow(() -> new FetchException("SOURCE_NOT_ALLOWED"));
            EventParser parser = parsers.get(source.parserType());
            if (parser == null) {
                throw new FetchException("PARSER_NOT_SUPPORTED");
            }
            String body = client.fetch(URI.create(source.baseUrl()));
            List<ParsedEvent> items = parser.parse(body, source.config(), props.maxItems());
            fetched = items.size();
            Instant now = Instant.now();
            for (int i = 0; i < items.size(); i++) {
                List<ValidationError> itemErrors = new ArrayList<>();
                Candidate c = validate(items.get(i), source, now, "items[" + i + "]", itemErrors);
                if (c == null) {
                    rejected++;
                    itemErrors.forEach(e -> addError(errors, e));
                } else if (repo.insertPendingEvent(Ids.newId(), source.id(), importId, c.title(), c.description(),
                        c.startsAt(), c.endsAt(), c.venue(), c.url(), c.fingerprint())) {
                    created++;
                } else {
                    duplicates++;
                }
            }
            repo.finish(importId, true, fetched, created, duplicates, rejected, errors);
        } catch (FetchException ex) {
            addError(errors, new ValidationError("source", ex.code()));
            repo.finish(importId, false, fetched, created, duplicates, rejected, errors);
        } catch (ParseException ex) {
            addError(errors, new ValidationError("source", "PARSE_ERROR"));
            repo.finish(importId, false, fetched, created, duplicates, rejected, errors);
        } catch (RuntimeException ex) {
            log.warn("Event import {} failed: {}", importId, ex.getClass().getSimpleName());   // never the content
            addError(errors, new ValidationError("source", "INTERNAL_ERROR"));
            repo.finish(importId, false, fetched, created, duplicates, rejected, errors);
        }
    }

    private record Candidate(String title, String description, Instant startsAt, Instant endsAt, String venue,
            String url, String fingerprint) {
    }

    /** @return the event ready to store, or null after adding the reasons to {@code errors} */
    private Candidate validate(ParsedEvent e, Source source, Instant now, String at, List<ValidationError> errors) {
        String title = e.title();
        if (title == null || title.isBlank()) {
            errors.add(new ValidationError(at + ".title", "Title is missing."));
        } else if (title.length() > MAX_TITLE) {
            errors.add(new ValidationError(at + ".title", "Title is longer than " + MAX_TITLE + " characters."));
        }
        if (e.startsAt() == null) {
            errors.add(new ValidationError(at + ".startsAt", "Start date is missing or not ISO-8601."));
        }
        if (e.endsAt() == null) {
            errors.add(new ValidationError(at + ".endsAt", "End date is missing or not ISO-8601."));
        }
        if (e.startsAt() != null && e.endsAt() != null) {
            if (e.endsAt().isBefore(e.startsAt())) {
                errors.add(new ValidationError(at + ".endsAt", "End date is before the start date."));
            } else if (e.endsAt().isBefore(now)) {
                errors.add(new ValidationError(at + ".endsAt", "The event has already ended."));
            }
        }
        String url = e.url() == null || e.url().isBlank() ? source.baseUrl() : e.url().trim();
        try {
            if (url.length() > MAX_URL) {
                throw new IllegalArgumentException();
            }
            OutboundUrlPolicy.requireAllowedUrl(url);                    // stored link must be a public https URL
        } catch (IllegalArgumentException ex) {
            errors.add(new ValidationError(at + ".url", "Event link must be a public https URL."));
        }
        if (!errors.isEmpty()) {
            return null;
        }
        String venue = e.venueName() != null ? e.venueName()
                : source.config().get("defaultVenueName") instanceof String s ? s : source.name();
        venue = truncate(venue, MAX_VENUE);
        String description = e.description() == null ? null : truncate(e.description(), MAX_DESCRIPTION);
        return new Candidate(title, description, e.startsAt(), e.endsAt(), venue, url,
                fingerprint(title, e.startsAt(), venue));
    }

    /** Lower-case hex SHA-256 of the normalised title, start instant and venue (cultural_events.fingerprint). */
    public static String fingerprint(String title, Instant startsAt, String venue) {
        String key = normalise(title) + "|" + startsAt + "|" + normalise(venue);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String normalise(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static void addError(List<ValidationError> errors, ValidationError error) {
        if (errors.size() < MAX_ERRORS) {
            errors.add(error);
        }
    }

    private static ImportView view(ImportRow row) {
        String status = switch (row.status()) {
            case "RUNNING" -> "PROCESSING";
            case "SUCCEEDED" -> "READY";
            default -> row.status();                                     // PENDING, FAILED
        };
        return new ImportView(row.id(), row.sourceId(), status, row.createdCount(), row.errors());
    }
}
