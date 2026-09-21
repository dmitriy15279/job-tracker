package com.example.jobtracker.persistence;

import com.example.jobtracker.persistence.entity.JobApplication;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class JobApplicationSpecifications {

    private JobApplicationSpecifications() {
    }

    public static Specification<JobApplication> filter(
            String company, String position, LocalDate appliedFrom, LocalDate appliedTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (company != null && !company.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("company")), "%" + company.toLowerCase() + "%"));
            }
            if (position != null && !position.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("position")), "%" + position.toLowerCase() + "%"));
            }
            if (appliedFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("appliedDate"), appliedFrom));
            }
            if (appliedTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("appliedDate"), appliedTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
