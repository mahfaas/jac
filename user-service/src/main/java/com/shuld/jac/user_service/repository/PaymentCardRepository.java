package com.shuld.jac.user_service.repository;

import com.shuld.jac.user_service.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {

    List<PaymentCard> findByUserId(Long userId);
    org.springframework.data.domain.Page<PaymentCard> findByUserId(Long userId, org.springframework.data.domain.Pageable pageable);

    long countByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE PaymentCard c SET c.active = :active WHERE c.id = :id")
    int setActive(@Param("id") Long id, @Param("active") boolean active);
}