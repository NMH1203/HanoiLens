package vn.edu.usth.hanoilens.modules.event.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import vn.edu.usth.hanoilens.modules.event.entity.CulturalEvent;
import vn.edu.usth.hanoilens.shared.Cursors;

/** Public event feed. Only APPROVED events are ever visible (imports stay PENDING_REVIEW until an admin acts). */
@Repository
public class CulturalEventQueryRepository {

    public enum Phase { ONGOING, UPCOMING, ENDED }

    public record Filter(Instant from, Instant to, Phase phase, Collection<UUID> destinationIds, String search,
            Cursors.Keyset after) {
    }

    @PersistenceContext
    private EntityManager em;

    public Optional<CulturalEvent> findApproved(UUID id) {
        return em.createQuery("select e from CulturalEvent e where e.id = :id and e.status = 'APPROVED'",
                CulturalEvent.class).setParameter("id", id).getResultStream().findFirst();
    }

    /** Ordered by startsAt then id; returns up to {@code limit} + 1 rows. */
    public List<CulturalEvent> findApproved(Filter f, Instant now, int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<CulturalEvent> q = cb.createQuery(CulturalEvent.class);
        Root<CulturalEvent> e = q.from(CulturalEvent.class);

        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(e.get("status"), CulturalEvent.STATUS_APPROVED));
        // Overlap with [from, to]
        where.add(cb.lessThanOrEqualTo(e.<Instant>get("startsAt"), f.to()));
        where.add(cb.greaterThanOrEqualTo(e.<Instant>get("endsAt"), f.from()));
        if (f.phase() != null) {
            switch (f.phase()) {
                case UPCOMING -> where.add(cb.greaterThan(e.<Instant>get("startsAt"), now));
                case ONGOING -> {
                    where.add(cb.lessThanOrEqualTo(e.<Instant>get("startsAt"), now));
                    where.add(cb.greaterThanOrEqualTo(e.<Instant>get("endsAt"), now));
                }
                case ENDED -> where.add(cb.lessThan(e.<Instant>get("endsAt"), now));
            }
        }
        if (f.destinationIds() != null) {
            if (f.destinationIds().isEmpty()) {
                where.add(cb.disjunction());                       // filter matched no destination -> no events
            } else {
                where.add(e.get("destinationId").in(f.destinationIds()));
            }
        }
        if (f.search() != null && !f.search().isBlank()) {
            String like = "%" + f.search().trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            where.add(cb.or(
                    cb.like(cb.lower(e.<String>get("title")), like, '\\'),
                    cb.like(cb.lower(e.<String>get("venueName")), like, '\\')));
        }
        if (f.after() != null) {
            where.add(cb.or(
                    cb.greaterThan(e.<Instant>get("startsAt"), f.after().at()),
                    cb.and(cb.equal(e.<Instant>get("startsAt"), f.after().at()),
                            cb.greaterThan(e.<UUID>get("id"), f.after().id()))));
        }
        q.select(e).where(where.toArray(new Predicate[0]))
                .orderBy(cb.asc(e.get("startsAt")), cb.asc(e.get("id")));
        return em.createQuery(q).setMaxResults(limit + 1).getResultList();
    }
}
