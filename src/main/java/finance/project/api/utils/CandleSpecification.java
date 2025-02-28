package finance.project.api.utils;

import finance.project.api.entities.Candle;
import finance.project.api.model.CandleFilterDTO;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class CandleSpecification implements Specification<Candle> {
    private CandleFilterDTO filter;

    @Override
    public Predicate toPredicate(Root<Candle> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter.getStartDate() != null && filter.getEndDate() != null) {
            predicates.add(cb.between(root.get("timestamp"), filter.getStartDate(), filter.getEndDate()));
        }
        if (filter.getSession() != null) {
            predicates.add(cb.equal(root.get("session"), filter.getSession()));
        }
        if (filter.getMarketCondition() != null) {
            predicates.add(cb.equal(root.get("marketCondition"), filter.getMarketCondition()));
        }
        if (filter.getNewsEvent() != null) {
            predicates.add(cb.equal(root.get("newsEvent"), filter.getNewsEvent()));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
