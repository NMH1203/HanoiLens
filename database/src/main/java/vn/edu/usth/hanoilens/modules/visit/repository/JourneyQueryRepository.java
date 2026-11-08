package vn.edu.usth.hanoilens.modules.visit.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import vn.edu.usth.hanoilens.modules.visit.entity.Journey;
import vn.edu.usth.hanoilens.shared.Cursors;

/** The caller's own visits, newest first (startedAt desc, id desc) with keyset paging. */
@Repository
public class JourneyQueryRepository {

    public record Filter(String status, UUID destinationId, Instant startedAfter, Instant startedBefore,
            Cursors.Keyset before) {
    }

    @PersistenceContext
    private EntityManager em;

    public List<Journey> findForUser(UUID userId, Filter f, int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Journey> q = cb.createQuery(Journey.class);
        Root<Journey> j = q.from(Journey.class);

        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(j.get("userId"), userId));
        if (f.status() != null) {
            where.add(cb.equal(j.get("status"), f.status()));
        }
        if (f.destinationId() != null) {
            where.add(cb.equal(j.get("destinationId"), f.destinationId()));
        }
        if (f.startedAfter() != null) {
            where.add(cb.greaterThanOrEqualTo(j.<Instant>get("startedAt"), f.startedAfter()));
        }
        if (f.startedBefore() != null) {
            where.add(cb.lessThanOrEqualTo(j.<Instant>get("startedAt"), f.startedBefore()));
        }
        if (f.before() != null) {
            where.add(cb.or(
                    cb.lessThan(j.<Instant>get("startedAt"), f.before().at()),
                    cb.and(cb.equal(j.<Instant>get("startedAt"), f.before().at()),
                            cb.lessThan(j.<UUID>get("id"), f.before().id()))));
        }
        q.select(j).where(where.toArray(new Predicate[0]))
                .orderBy(cb.desc(j.get("startedAt")), cb.desc(j.get("id")));
        return em.createQuery(q).setMaxResults(limit + 1).getResultList();
    }
}
