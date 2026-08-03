package com.shuld.jac.authservice.repository;

import com.shuld.jac.authservice.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    Optional<Credential> findByLogin(String login);

    Optional<Credential> findByUserId(Long userId);
}
