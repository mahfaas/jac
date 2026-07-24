package com.shuld.jac.user_service.repository.specification;

import com.shuld.jac.user_service.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> hasName(String name) {
        return (root, query, cb) ->
                (name == null || name.isBlank())
                        ? null
                        : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<User> hasSurname(String surname) {
        return (root, query, cb) ->
                (surname == null || surname.isBlank())
                        ? null
                        : cb.like(cb.lower(root.get("surname")), "%" + surname.toLowerCase() + "%");
    }

    public static Specification<User> filterBy(String name, String surname) {
        return Specification.where(hasName(name)).and(hasSurname(surname));
    }
}