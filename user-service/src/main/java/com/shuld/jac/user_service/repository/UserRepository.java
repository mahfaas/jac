package com.shuld.jac.user_service.repository;

import com.shuld.jac.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET active = :active WHERE id = :id", nativeQuery = true)
    int setActive(@Param("id") Long id, @Param("active") boolean active);
}