package com.example.jobtracker.persistence;

import com.example.jobtracker.persistence.entity.User;
import com.example.jobtracker.persistence.entity.UserType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> filter(
            String firstName, String lastName, String email, UserType userType,
            LocalDate birthDateFrom, LocalDate birthDateTo, UUID companyId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (firstName != null && !firstName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%"));
            }
            if (lastName != null && !lastName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%"));
            }
            if (email != null && !email.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            if (userType != null) {
                predicates.add(cb.equal(root.get("userType"), userType));
            }
            if (birthDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("birthDate"), birthDateFrom));
            }
            if (birthDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("birthDate"), birthDateTo));
            }
            if (companyId != null) {
                // (user_id, company_id) is the join table's primary key, so this join yields at most one row per user
                predicates.add(cb.equal(root.join("companies").get("id"), companyId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
