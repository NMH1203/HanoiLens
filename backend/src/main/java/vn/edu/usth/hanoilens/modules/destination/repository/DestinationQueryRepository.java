package vn.edu.usth.hanoilens.modules.destination.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Repository;
import vn.edu.usth.hanoilens.modules.destination.entity.Destination;

/** Filtered + sorted public catalogue listing with offset paging. Only PUBLISHED rows are ever returned. */
@Repository
public class DestinationQueryRepository {

    public enum Sort { FEATURED, NAME, NEWEST }

    public record Filter(String area, String district, Boolean featured, String search, Sort sort) {
    }

    @PersistenceContext
    private EntityManager em;

    /** Returns up to {@code limit} + 1 rows so the caller can tell whether another page exists. */
    public List<Destination> findPublished(Filter filter, int offset, int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Destination> query = cb.createQuery(Destination.class);
        Root<Destination> d = query.from(Destination.class);

        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(d.get("status"), Destination.STATUS_PUBLISHED));
        if (filter.area() != null) {
            where.add(cb.equal(d.get("area"), filter.area()));
        }
        if (filter.district() != null) {
            where.add(cb.equal(cb.lower(d.<String>get("district")), filter.district().toLowerCase(Locale.ROOT)));
        }
        if (filter.featured() != null) {
            where.add(cb.equal(d.get("featured"), filter.featured()));
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            String like = "%" + escapeLike(filter.search().trim().toLowerCase(Locale.ROOT)) + "%";
            where.add(cb.or(
                    cb.like(cb.lower(d.<String>get("name")), like, '\\'),
                    cb.like(cb.lower(d.<String>get("summary")), like, '\\')));
        }
        query.select(d).where(where.toArray(new Predicate[0]));

        List<Order> order = new ArrayList<>();
        switch (filter.sort()) {
            case NAME -> order.add(cb.asc(d.get("name")));
            case NEWEST -> order.add(cb.desc(d.get("createdAt")));
            default -> {
                order.add(cb.desc(d.get("featured")));
                order.add(cb.asc(d.get("name")));
            }
        }
        order.add(cb.asc(d.get("id")));                        // stable tie-breaker for paging
        query.orderBy(order);

        return em.createQuery(query).setFirstResult(offset).setMaxResults(limit + 1).getResultList();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
