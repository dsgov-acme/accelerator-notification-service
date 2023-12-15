package io.nuvalence.platform.notification.service.model;

import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Filter for searching templates.
 */
@SuperBuilder
public class SearchTemplateFilter extends BaseFilter {
    private String name;

    /**
     * Returns a Specification object based on the filter's name.
     *
     * @return Specification object
     */
    public Specification<MessageTemplate> getTemplateSpecifications() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + name.toLowerCase(Locale.US) + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
