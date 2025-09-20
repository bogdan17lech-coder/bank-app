package com.example.bank.rest.account;

import com.example.bank.rest.account.dto.AccountDto;
import com.example.bank.rest.account.dto.AmountRequest;
import com.example.bank.rest.account.dto.NewAccountRequest;
import com.example.bank.rest.account.dto.TransactionDto;
import com.example.bank.rest.account.dto.TransferRequest;

// Transactions: import only entity/repo (keep service logic here)
import com.example.bank.rest.transaction.TransactionEntity;
import com.example.bank.rest.transaction.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

/**
 * Account domain service (REST side): mapping, validations, money ops.
 * Keep logic simple and explicit for interview readability.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepo;
    private final TransactionRepository trxRepo;

    public AccountService(AccountRepository accountRepo, TransactionRepository trxRepo) {
        this.accountRepo = accountRepo;
        this.trxRepo = trxRepo;
    }

    // ---------- Mapping helpers ----------
    private AccountDto toDto(AccountEntity e) {
        AccountDto d = new AccountDto();
        d.setId(e.getId());
        d.setCustomerId(e.getCustomerId());
        d.setNumber(e.getNumber());
        d.setCurrency(e.getCurrency());
        d.setBalance(e.getBalance());
        return d;
    }

    private TransactionDto toDto(TransactionEntity t) {
        TransactionDto d = new TransactionDto();
        d.setAccountId(t.getAccount().getId());
        d.setType(t.getType().name());
        d.setAmount(t.getAmount());
        d.setDescription(t.getDescription());
        // Use OffsetDateTime (UTC) for API responses
        d.setCreatedAt(OffsetDateTime.ofInstant(t.getCreatedAt(), ZoneOffset.UTC));
        return d;
    }

    // ---------- Queries ----------
    public List<AccountDto> listByCustomer(long customerId) {
        return accountRepo.findByCustomerId(customerId).stream().map(this::toDto).toList();
    }

    public AccountDto getByCustomer(long customerId, long accountId) {
        AccountEntity e = accountRepo.findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "account not found"));
        return toDto(e);
    }

    // Public read (used by MVC optional currency check)
    public AccountDto getPublic(long accountId) {
        AccountEntity e = accountRepo.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "account not found"));
        return toDto(e);
    }

    public List<TransactionDto> listTransactions(long customerId, long accountId) {
        // Ensure ownership before listing
        accountRepo.findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "account not found"));
        return trxRepo.findTop100ByAccount_IdOrderByCreatedAtDesc(accountId)
                .stream().map(this::toDto).toList();
    }

    // ---------- Commands ----------
    @Transactional // one DB unit: create account
    public AccountDto create(long customerId, NewAccountRequest req) {
        if (req.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "balance cannot be negative");
        }
        AccountEntity e = new AccountEntity();
        e.setCustomerId(customerId);
        e.setNumber(req.getNumber());
        e.setCurrency(req.getCurrency());
        e.setBalance(req.getBalance());
        e = accountRepo.save(e);
        return toDto(e);
    }

    @Transactional
    public void delete(long customerId, long accountId) {
        AccountEntity e = accountRepo.findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "account not found"));

        // junior: можно удалять только при нулевом балансе
        if (e.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(CONFLICT, "balance must be 0 to delete");
        }

        // NEW: сначала удаляем "детей" (transactions), иначе FK не даст удалить аккаунт
        trxRepo.deleteByAccount_Id(accountId);  // junior: safe for MySQL FK

        // теперь удаляем сам счёт
        accountRepo.delete(e);
    }

    @Transactional // one DB unit: deposit + transaction
    public TransactionDto deposit(long customerId, long accountId, AmountRequest req) {
        if (req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "amount must be positive");
        }
        AccountEntity acc = accountRepo.findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "account not found"));

        acc.setBalance(acc.getBalance().add(req.getAmount()));

        TransactionEntity t = new TransactionEntity();
        t.setAccount(acc);
        t.setType(com.example.bank.rest.transaction.TransactionType.DEPOSIT);
        t.setAmount(req.getAmount());
        t.setDescription(req.getDescription());
        t = trxRepo.save(t);

        return toDto(t);
    }

    @Transactional // one DB unit: withdraw + transaction
    public TransactionDto withdraw(long customerId, long accountId, AmountRequest req) {
        if (req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "amount must be positive");
        }
        AccountEntity acc = accountRepo.findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "account not found"));

        if (acc.getBalance().compareTo(req.getAmount()) < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "insufficient funds");
        }

        acc.setBalance(acc.getBalance().subtract(req.getAmount()));

        TransactionEntity t = new TransactionEntity();
        t.setAccount(acc);
        t.setType(com.example.bank.rest.transaction.TransactionType.WITHDRAW);
        t.setAmount(req.getAmount());
        t.setDescription(req.getDescription());
        t = trxRepo.save(t);

        return toDto(t);
    }

    @Transactional // one DB unit: move money + two transactions
    public TransactionDto transfer(long customerId, long fromAccountId, TransferRequest req) {
        if (req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "amount must be positive");
        }
        AccountEntity from = accountRepo.findByIdAndCustomerId(fromAccountId, customerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "from account not found"));

        AccountEntity to = accountRepo.findById(req.getToAccountId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "to account not found"));

        // Simple same-currency rule (no FX here)
        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new ResponseStatusException(BAD_REQUEST, "currencies must match");
        }
        if (from.getBalance().compareTo(req.getAmount()) < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "insufficient funds");
        }

        // Update balances
        from.setBalance(from.getBalance().subtract(req.getAmount()));
        to.setBalance(to.getBalance().add(req.getAmount()));

        // Outgoing record
        TransactionEntity out = new TransactionEntity();
        out.setAccount(from);
        out.setType(com.example.bank.rest.transaction.TransactionType.TRANSFER_OUT);
        out.setAmount(req.getAmount());
        out.setDescription(req.getDescription());
        trxRepo.save(out);

        // Incoming record
        TransactionEntity in = new TransactionEntity();
        in.setAccount(to);
        in.setType(com.example.bank.rest.transaction.TransactionType.TRANSFER_IN);
        in.setAmount(req.getAmount());
        in.setDescription(req.getDescription());
        trxRepo.save(in);

        return toDto(out);
    }
}
