package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.port.out.AccountRepository;
import com.finanzasia.domain.port.out.CategoryRepository;
import com.finanzasia.domain.port.out.ExchangeRateRepository;
import com.finanzasia.domain.port.out.ReportRepository;
import com.finanzasia.domain.port.out.TagRepository;
import com.finanzasia.domain.port.out.TransactionRepository;
import com.finanzasia.infrastructure.redis.RedisTokenStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Wires all infrastructure persistence adapters as Spring beans.
 *
 * Kept in one place so the hexagonal boundary is explicit:
 * domain ports -> these beans -> JPA Spring Data interfaces.
 */
@Configuration
public class PersistenceConfig {

    @Bean
    public CategoryRepository categoryRepository(JpaCategoryRepositoryPort jpaPort) {
        return new JpaCategoryRepository(jpaPort);
    }

    @Bean
    public AccountRepository accountRepository(JpaAccountRepositoryPort jpaPort) {
        return new JpaAccountRepository(jpaPort);
    }

    @Bean
    public TagRepository tagRepository(JpaTagRepositoryPort jpaTagPort) {
        return new JpaTagRepository(jpaTagPort);
    }

    @Bean
    public TransactionRepository transactionRepository(
            JpaTransactionRepositoryPort jpaPort,
            JpaTagRepositoryPort jpaTagPort) {
        return new JpaTransactionRepository(jpaPort, jpaTagPort);
    }

    @Bean
    public UserMapper userMapper() {
        return new UserMapper();
    }

    @Bean
    public JpaUserRepository userRepository(JpaUserRepositoryPort jpaPort, UserMapper mapper) {
        return new JpaUserRepository(jpaPort, mapper);
    }

    @Bean
    public ReportRepository reportRepository(JpaReportRepositoryPort jpaPort) {
        return new JpaReportRepositoryAdapter(jpaPort);
    }

    @Bean
    public ExchangeRateRepository exchangeRateRepository(JpaExchangeRateRepositoryPort jpaPort) {
        return new JpaExchangeRateRepository(jpaPort);
    }

    @Bean
    public RedisTokenStore tokenStore(StringRedisTemplate redisTemplate) {
        return new RedisTokenStore(redisTemplate);
    }
}
