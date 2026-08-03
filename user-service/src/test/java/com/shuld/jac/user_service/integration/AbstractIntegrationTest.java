package com.shuld.jac.user_service.integration;

import com.redis.testcontainers.RedisContainer;
import com.shuld.jac.jwtcommon.JwtService;
import com.shuld.jac.user_service.repository.PaymentCardRepository;
import com.shuld.jac.user_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "JWT_SECRET=test-secret-key-with-at-least-32-characters!!")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @ServiceConnection
    static final RedisContainer redis = new RedisContainer("redis:7");

    static {
        postgres.start();
        redis.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PaymentCardRepository cardRepository;

    @Autowired
    protected CacheManager cacheManager;

    @Autowired
    protected JwtService jwtService;

    @AfterEach
    void cleanupDatabaseAndCache() {
        cardRepository.deleteAll();
        userRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    protected String adminToken() {
        return "Bearer " + jwtService.generateAccessToken(999_000_000L, "ADMIN");
    }

    protected String userToken(Long userId) {
        return "Bearer " + jwtService.generateAccessToken(userId, "USER");
    }
}
