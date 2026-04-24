package com.finanzasia.application.service;

import com.finanzasia.domain.model.ExchangeRate;
import com.finanzasia.domain.port.out.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    private ExchangeRateService service;

    private static final String FROM = "USD";
    private static final String TO   = "PEN";

    @BeforeEach
    void setUp() {
        service = new ExchangeRateService(exchangeRateRepository);
    }

    // --- helpers ---

    private ExchangeRate buildRate(BigDecimal buy, BigDecimal sell) {
        Instant now = Instant.now();
        return new ExchangeRate(
                UUID.randomUUID(), FROM, TO, buy, sell,
                LocalDate.now(), "MANUAL", now, now);
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getOrCreateDefault")
    class GetOrCreateDefault {

        @Test
        @DisplayName("returns existing rate when one is already recorded for today")
        void getOrCreateDefaultExistingRateReturnsIt() {
            ExchangeRate existing = buildRate(new BigDecimal("3.70"), new BigDecimal("3.75"));
            when(exchangeRateRepository.findByDate(eq(FROM), eq(TO), any(LocalDate.class)))
                    .thenReturn(Optional.of(existing));

            ExchangeRate result = service.getOrCreateDefault();

            assertThat(result).isSameAs(existing);
            verify(exchangeRateRepository, never()).upsert(any());
        }

        @Test
        @DisplayName("creates and persists a default rate when none exists for today")
        void getOrCreateDefaultNoRateCreatesDefault() {
            when(exchangeRateRepository.findByDate(eq(FROM), eq(TO), any(LocalDate.class)))
                    .thenReturn(Optional.empty());
            when(exchangeRateRepository.upsert(any())).thenAnswer(inv -> inv.getArgument(0));

            ExchangeRate result = service.getOrCreateDefault();

            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateRepository).upsert(captor.capture());

            ExchangeRate persisted = captor.getValue();
            assertThat(persisted.getCurrencyFrom()).isEqualTo(FROM);
            assertThat(persisted.getCurrencyTo()).isEqualTo(TO);
            assertThat(persisted.getBuyRate()).isEqualByComparingTo(ExchangeRateService.DEFAULT_BUY_RATE);
            assertThat(persisted.getSellRate()).isEqualByComparingTo(ExchangeRateService.DEFAULT_SELL_RATE);
            assertThat(persisted.getSource()).isEqualTo(ExchangeRateService.DEFAULT_SOURCE);
            assertThat(persisted.getId()).isNotNull();

            assertThat(result.getBuyRate()).isEqualByComparingTo(ExchangeRateService.DEFAULT_BUY_RATE);
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("upsertToday")
    class UpsertToday {

        @Test
        @DisplayName("updates buy and sell rates on an existing record")
        void upsertTodayExistingRecordUpdatesRates() {
            ExchangeRate existing = buildRate(new BigDecimal("3.69"), new BigDecimal("3.74"));
            when(exchangeRateRepository.findByDate(eq(FROM), eq(TO), any(LocalDate.class)))
                    .thenReturn(Optional.of(existing));
            when(exchangeRateRepository.upsert(any())).thenAnswer(inv -> inv.getArgument(0));

            BigDecimal newBuy  = new BigDecimal("3.71");
            BigDecimal newSell = new BigDecimal("3.76");

            ExchangeRate result = service.upsertToday(newBuy, newSell);

            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateRepository).upsert(captor.capture());

            assertThat(captor.getValue().getBuyRate()).isEqualByComparingTo(newBuy);
            assertThat(captor.getValue().getSellRate()).isEqualByComparingTo(newSell);
            assertThat(result.getBuyRate()).isEqualByComparingTo(newBuy);
            assertThat(result.getSellRate()).isEqualByComparingTo(newSell);
        }

        @Test
        @DisplayName("creates a new record when none exists for today")
        void upsertTodayNoExistingRecordCreatesNew() {
            when(exchangeRateRepository.findByDate(eq(FROM), eq(TO), any(LocalDate.class)))
                    .thenReturn(Optional.empty());
            when(exchangeRateRepository.upsert(any())).thenAnswer(inv -> inv.getArgument(0));

            BigDecimal buy  = new BigDecimal("3.72");
            BigDecimal sell = new BigDecimal("3.77");

            ExchangeRate result = service.upsertToday(buy, sell);

            ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
            verify(exchangeRateRepository).upsert(captor.capture());

            ExchangeRate persisted = captor.getValue();
            assertThat(persisted.getCurrencyFrom()).isEqualTo(FROM);
            assertThat(persisted.getCurrencyTo()).isEqualTo(TO);
            assertThat(persisted.getBuyRate()).isEqualByComparingTo(buy);
            assertThat(persisted.getSellRate()).isEqualByComparingTo(sell);
            assertThat(persisted.getSource()).isEqualTo(ExchangeRateService.DEFAULT_SOURCE);
            assertThat(persisted.getId()).isNotNull();
            assertThat(result.getSellRate()).isEqualByComparingTo(sell);
        }
    }
}
