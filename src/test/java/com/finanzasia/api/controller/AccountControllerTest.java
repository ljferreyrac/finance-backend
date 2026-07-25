package com.finanzasia.api.controller;

import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.exceptions.AccountInUseException;
import com.finanzasia.domain.exceptions.AccountNotFoundException;
import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.AccountDetail;
import com.finanzasia.domain.model.AccountType;
import com.finanzasia.domain.model.NetWorth;
import com.finanzasia.domain.port.in.AccountUseCase;
import com.finanzasia.domain.port.in.AuthenticateAccessTokenUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountUseCase accountUseCase;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessToken;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    private UserPrincipal principal;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(USER_ID, "user@example.com");
    }

    private AccountDetail buildDetail(BigDecimal availableCredit) {
        Instant now = Instant.now();
        Account account = new Account(ACCOUNT_ID, USER_ID, "BCP Soles", AccountType.BANK, "BCP", "PEN",
                new BigDecimal("1000.00"), null, null, null, "#0066CC", false, true, null, now, now);
        return new AccountDetail(account, 5, availableCredit);
    }

    @Test
    @DisplayName("GET /api/v1/accounts returns the caller's accounts")
    void listAccountsReturnsOwnedAccounts() throws Exception {
        when(accountUseCase.listAccounts(USER_ID)).thenReturn(List.of(buildDetail(null)));

        mockMvc.perform(get("/api/v1/accounts").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$[0].transactionCount").value(5));
    }

    @Test
    @DisplayName("GET /api/v1/accounts without authentication is rejected with 401")
    void listAccountsWithoutAuthIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/net-worth defaults includeDebt to true when omitted")
    void getNetWorthDefaultsIncludeDebtToTrue() throws Exception {
        when(accountUseCase.getNetWorth(USER_ID, true))
                .thenReturn(new NetWorth(new BigDecimal("1000.00"), BigDecimal.ZERO, List.of(buildDetail(null))));

        mockMvc.perform(get("/api/v1/accounts/net-worth").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPEN").value(1000.00));

        verify(accountUseCase).getNetWorth(USER_ID, true);
    }

    @Test
    @DisplayName("GET /api/v1/accounts/net-worth?includeDebt=false forwards the flag")
    void getNetWorthForwardsIncludeDebtFalse() throws Exception {
        when(accountUseCase.getNetWorth(USER_ID, false))
                .thenReturn(new NetWorth(BigDecimal.ZERO, BigDecimal.ZERO, List.of()));

        mockMvc.perform(get("/api/v1/accounts/net-worth")
                        .param("includeDebt", "false")
                        .with(user(principal)))
                .andExpect(status().isOk());

        verify(accountUseCase).getNetWorth(USER_ID, false);
    }

    @Test
    @DisplayName("POST /api/v1/accounts creates an account and returns 201")
    void createAccountReturns201() throws Exception {
        when(accountUseCase.createAccount(eq(USER_ID), eq("BCP Soles"), eq(AccountType.BANK), eq("BCP"),
                eq("PEN"), any(), any(), any(), any(), any(), eq(false), any()))
                .thenReturn(buildDetail(null));

        mockMvc.perform(post("/api/v1/accounts")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"BCP Soles\",\"type\":\"BANK\",\"bank\":\"BCP\",\"currency\":\"PEN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ACCOUNT_ID.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/accounts with a blank name fails bean validation with 400")
    void createAccountWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"BANK\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/accounts/{id} updates an account and returns 200")
    void updateAccountReturns200() throws Exception {
        when(accountUseCase.updateAccount(eq(USER_ID), eq(ACCOUNT_ID), eq("Nueva"), any(), any(), any(), any(),
                any(), any(), any()))
                .thenReturn(buildDetail(null));

        mockMvc.perform(put("/api/v1/accounts/" + ACCOUNT_ID)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nueva\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/accounts/{id} for an account that does not exist surfaces as 404")
    void updateUnknownAccountReturns404() throws Exception {
        when(accountUseCase.updateAccount(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new AccountNotFoundException(ACCOUNT_ID));

        mockMvc.perform(put("/api/v1/accounts/" + ACCOUNT_ID)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nueva\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/accounts/{id} deletes an account and returns 204")
    void deleteAccountReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/accounts/" + ACCOUNT_ID).with(user(principal)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(accountUseCase).deleteAccount(USER_ID, ACCOUNT_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/accounts/{id} with existing transactions surfaces as 409")
    void deleteAccountInUseReturns409() throws Exception {
        org.mockito.Mockito.doThrow(new AccountInUseException(ACCOUNT_ID, 3))
                .when(accountUseCase).deleteAccount(any(), any());

        mockMvc.perform(delete("/api/v1/accounts/" + ACCOUNT_ID).with(user(principal)).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /api/v1/accounts/{id}/default sets the account as default and returns 200")
    void setDefaultAccountReturns200() throws Exception {
        when(accountUseCase.setDefaultAccount(USER_ID, ACCOUNT_ID)).thenReturn(buildDetail(null));

        mockMvc.perform(patch("/api/v1/accounts/" + ACCOUNT_ID + "/default")
                        .with(user(principal))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACCOUNT_ID.toString()));
    }
}
