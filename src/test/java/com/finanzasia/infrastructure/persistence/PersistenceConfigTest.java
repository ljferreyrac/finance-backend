package com.finanzasia.infrastructure.persistence;

import com.finanzasia.infrastructure.redis.RedisTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every {@code @Bean} method here is a plain constructor call, so each one can be
 * invoked directly without starting a Spring context.
 */
@ExtendWith(MockitoExtension.class)
class PersistenceConfigTest {

    private final PersistenceConfig config = new PersistenceConfig();

    @Mock private JpaCategoryRepositoryPort categoryPort;
    @Mock private JpaAccountRepositoryPort accountPort;
    @Mock private JpaTagRepositoryPort tagPort;
    @Mock private JpaTransactionRepositoryPort transactionPort;
    @Mock private JpaUserRepositoryPort userPort;
    @Mock private JpaReportRepositoryPort reportPort;
    @Mock private JpaExchangeRateRepositoryPort exchangeRatePort;
    @Mock private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("categoryRepository wraps the JPA port in a JpaCategoryRepository")
    void categoryRepositoryBean() {
        assertThat(config.categoryRepository(categoryPort)).isInstanceOf(JpaCategoryRepository.class);
    }

    @Test
    @DisplayName("accountRepository wraps the JPA port in a JpaAccountRepository")
    void accountRepositoryBean() {
        assertThat(config.accountRepository(accountPort)).isInstanceOf(JpaAccountRepository.class);
    }

    @Test
    @DisplayName("tagRepository wraps the JPA port in a JpaTagRepository")
    void tagRepositoryBean() {
        assertThat(config.tagRepository(tagPort)).isInstanceOf(JpaTagRepository.class);
    }

    @Test
    @DisplayName("transactionRepository wraps both JPA ports in a JpaTransactionRepository")
    void transactionRepositoryBean() {
        assertThat(config.transactionRepository(transactionPort, tagPort))
                .isInstanceOf(JpaTransactionRepository.class);
    }

    @Test
    @DisplayName("userMapper builds a plain UserMapper")
    void userMapperBean() {
        assertThat(config.userMapper()).isInstanceOf(UserMapper.class);
    }

    @Test
    @DisplayName("userRepository wraps the JPA port and mapper in a JpaUserRepository")
    void userRepositoryBean() {
        assertThat(config.userRepository(userPort, config.userMapper())).isInstanceOf(JpaUserRepository.class);
    }

    @Test
    @DisplayName("reportRepository wraps the JPA port in a JpaReportRepositoryAdapter")
    void reportRepositoryBean() {
        assertThat(config.reportRepository(reportPort)).isInstanceOf(JpaReportRepositoryAdapter.class);
    }

    @Test
    @DisplayName("exchangeRateRepository wraps the JPA port in a JpaExchangeRateRepository")
    void exchangeRateRepositoryBean() {
        assertThat(config.exchangeRateRepository(exchangeRatePort)).isInstanceOf(JpaExchangeRateRepository.class);
    }

    @Test
    @DisplayName("tokenStore wraps the Redis template in a RedisTokenStore")
    void tokenStoreBean() {
        assertThat(config.tokenStore(redisTemplate)).isInstanceOf(RedisTokenStore.class);
    }
}
