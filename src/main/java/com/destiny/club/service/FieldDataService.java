package com.destiny.club.service;

import com.destiny.club.domain.accounting.GLAccount;
import com.destiny.club.domain.accounting.GLCodes;
import com.destiny.club.domain.accounting.JournalEntryLine;
import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.fielddata.FieldData;
import com.destiny.club.domain.savings.SavingsAccount;
import com.destiny.club.domain.savings.SavingsProduct;
import com.destiny.club.domain.savings.SavingsTransaction;
import com.destiny.club.exception.BusinessException;
import com.destiny.club.exception.NotFoundException;
import com.destiny.club.repository.FieldDataRepository;
import com.destiny.club.repository.SavingsTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Backs the Field Data screen: a field officer collects a savings deposit out in the field.
 * Unlike every other savings deposit in the system, the cash doesn't land straight in a till or
 * bank account - it's booked against a dedicated "Field Cash Control" holding account, since the
 * field team is physically carrying it until it's later banked. Any officer can collect from any
 * member regardless of which permanent group that member belongs to; the two-person collection
 * team rotates daily/weekly and is recorded here as plain names rather than system users.
 */
@Service
@RequiredArgsConstructor
public class FieldDataService {

    private final FieldDataRepository fieldDataRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;
    private final SavingsService savingsService;
    private final AccountingService accountingService;

    public List<FieldData> findAll() {
        return fieldDataRepository.findAllOrderByDateDesc();
    }

    public List<FieldData> findBetween(LocalDate fromDate, LocalDate toDate) {
        return fieldDataRepository.findByTransactionDateBetween(fromDate, toDate);
    }

    public List<String> knownCollectorNames() {
        return fieldDataRepository.findDistinctCollectorNames();
    }

    public FieldData getById(Long id) {
        return fieldDataRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Field data entry not found: " + id));
    }

    @Transactional
    public FieldData record(Client client, SavingsProduct product, BigDecimal amount, LocalDate date,
                             String collectorOneName, String collectorTwoName, String narration, String createdBy) {
        if (collectorOneName == null || collectorOneName.isBlank()) {
            throw new BusinessException("At least one field officer's name is required");
        }

        SavingsAccount account = savingsService.findOrOpenAccount(client, product);
        SavingsTransaction txn = savingsService.recordDeposit(account, amount, date, narration, createdBy);

        GLAccount fieldCashControl = accountingService.getAccountByCode(GLCodes.FIELD_CASH_CONTROL);
        GLAccount savingsControl = accountingService.getAccountByCode(GLCodes.MEMBER_SAVINGS);

        var entry = accountingService.post(txn.getTransactionDate(), "FIELD_COLLECTION", txn.getId(),
                "Field collection - " + account.getAccountNumber() + " - " + client.getFullName(),
                createdBy,
                List.of(
                        JournalEntryLine.debit(fieldCashControl, amount, "Collected in the field - " + account.getAccountNumber()),
                        JournalEntryLine.credit(savingsControl, amount, "Deposit to " + account.getAccountNumber())
                ));
        txn.setJournalEntryId(entry.getId());
        savingsTransactionRepository.save(txn);

        FieldData fieldData = new FieldData();
        fieldData.setClient(client);
        fieldData.setSavingsTransaction(txn);
        fieldData.setCollectorOneName(collectorOneName.trim());
        fieldData.setCollectorTwoName(collectorTwoName != null && !collectorTwoName.isBlank() ? collectorTwoName.trim() : null);
        fieldData.setCreatedBy(createdBy);
        return fieldDataRepository.save(fieldData);
    }

    @Transactional
    public FieldData updateNarration(Long id, String narration, String updatedBy) {
        FieldData fieldData = getById(id);
        savingsService.updateNarration(fieldData.getSavingsTransaction().getId(), narration, updatedBy);
        return fieldData;
    }

    /** Voids the underlying savings deposit (reversing journal entry + recomputed balance) - the field data record itself stays, just pointing at a now-voided transaction. */
    @Transactional
    public FieldData voidEntry(Long id, String voidedBy, String reason) {
        FieldData fieldData = getById(id);
        savingsService.voidTransaction(fieldData.getSavingsTransaction().getId(), voidedBy, reason);
        return fieldData;
    }
}
