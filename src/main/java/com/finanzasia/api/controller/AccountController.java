package com.finanzasia.api.controller;

import com.finanzasia.api.dto.AccountDTO;
import com.finanzasia.api.dto.CreateAccountRequest;
import com.finanzasia.api.dto.NetWorthDTO;
import com.finanzasia.api.dto.UpdateAccountRequest;
import com.finanzasia.domain.model.AccountType;
import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.NetWorth;
import java.math.BigDecimal;
import com.finanzasia.domain.port.in.AccountUseCase;
import com.finanzasia.domain.port.out.AccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Accounts", description = "Manage financial accounts (checking, savings, credit cards, cash, wallets)")
@RestController
@RequestMapping("/api/v1/accounts")
@Validated
public class AccountController {

    private final AccountUseCase accountUseCase;
    private final AccountRepository accountRepository;

    public AccountController(AccountUseCase accountUseCase, AccountRepository accountRepository) {
        this.accountUseCase = accountUseCase;
        this.accountRepository = accountRepository;
    }

    @Operation(summary = "List accounts", description = "Returns all accounts owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account list",
                         content = @Content(schema = @Schema(implementation = AccountDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    @GetMapping
    public List<AccountDTO> listAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        return accountUseCase.listAccounts(principal.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Operation(summary = "Get net worth", description = "Returns total balances by currency and the full account list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Net worth snapshot",
                         content = @Content(schema = @Schema(implementation = NetWorthDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    @GetMapping("/net-worth")
    public NetWorthDTO getNetWorth(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "includeDebt", defaultValue = "true") boolean includeDebt) {
        NetWorth netWorth = accountUseCase.getNetWorth(principal.getId(), includeDebt);
        List<AccountDTO> accounts = netWorth.accounts().stream().map(this::toDTO).toList();
        return new NetWorthDTO(netWorth.totalPEN(), netWorth.totalUSD(), accounts);
    }

    @Operation(summary = "Create account", description = "Creates a new financial account for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                         content = @Content(schema = @Schema(implementation = AccountDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDTO createAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAccountRequest request) {

        Account account = accountUseCase.createAccount(
                principal.getId(),
                request.name(),
                request.type(),
                request.bank(),
                request.currency(),
                request.initialBalance(),
                request.creditLimit(),
                request.closingDay(),
                request.dueDay(),
                request.color(),
                request.isDefault(),
                request.linkedAccountId());

        return toDTO(account);
    }

    @Operation(summary = "Update account",
               description = "Updates an existing account. Type and currency are immutable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account updated",
                         content = @Content(schema = @Schema(implementation = AccountDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    })
    @PutMapping("/{id}")
    public AccountDTO updateAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Account UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountRequest request) {

        Account account = accountUseCase.updateAccount(
                principal.getId(),
                id,
                request.name(),
                request.bank(),
                request.creditLimit(),
                request.closingDay(),
                request.dueDay(),
                request.color(),
                request.isActive(),
                request.linkedAccountId());

        return toDTO(account);
    }

    @Operation(summary = "Delete account",
               description = "Deletes an account. Returns 409 if the account has existing transactions.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Account not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Account has transactions", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Account UUID") @PathVariable UUID id) {

        accountUseCase.deleteAccount(principal.getId(), id);
    }

    @Operation(summary = "Set default account",
               description = "Marks the given account as the user's default, clearing any previous default.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default updated",
                         content = @Content(schema = @Schema(implementation = AccountDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    })
    @PatchMapping("/{id}/default")
    public AccountDTO setDefaultAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Account UUID") @PathVariable UUID id) {

        Account account = accountUseCase.setDefaultAccount(principal.getId(), id);
        return toDTO(account);
    }

    private AccountDTO toDTO(Account account) {
        long txCount = accountRepository.countTransactionsByAccountId(account.getId());
        BigDecimal availableCredit = computeAvailableCredit(account);
        return new AccountDTO(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getBank(),
                account.getCurrency(),
                account.getCurrentBalance(),
                account.getCreditLimit(),
                account.getClosingDay(),
                account.getDueDay(),
                account.getColor(),
                account.isDefault(),
                account.isActive(),
                txCount,
                account.getLinkedAccountId(),
                availableCredit);
    }

    /**
     * Available credit is {@code creditLimit - |currentBalance|}; null for non-credit accounts
     * or when no limit is set. currentBalance is assumed PEN-equivalent.
     */
    private BigDecimal computeAvailableCredit(Account account) {
        if (account.getType() != AccountType.CREDIT_CARD || account.getCreditLimit() == null) {
            return null;
        }
        BigDecimal outstanding = account.getCurrentBalance().abs();
        return account.getCreditLimit().subtract(outstanding);
    }
}
