package com.destiny.club.web;

import com.destiny.club.security.CustomUserDetails;
import com.destiny.club.service.AccountingService;
import com.destiny.club.service.ClientService;
import com.destiny.club.service.MemberStatementService;
import com.destiny.club.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final AccountingService accountingService;
    private final PdfExportService pdfExportService;
    private final MemberStatementService memberStatementService;
    private final ClientService clientService;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @GetMapping("/trial-balance")
    public String trialBalance(@RequestParam(required = false) LocalDate asOfDate, Model model) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        model.addAttribute("report", accountingService.generateTrialBalance(date));
        model.addAttribute("asOfDate", date);
        return "reports/trial-balance";
    }

    @GetMapping(value = "/trial-balance/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> trialBalancePdf(@RequestParam(required = false) LocalDate asOfDate,
                                                   @AuthenticationPrincipal CustomUserDetails principal) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        Map<String, Object> model = new HashMap<>();
        model.put("report", accountingService.generateTrialBalance(date));
        model.put("asOfDate", date);
        addGenerationMeta(model, principal);
        byte[] pdf = pdfExportService.renderPdf("reports/pdf/trial-balance-pdf", model);
        return pdfResponse(pdf, "trial-balance-" + date + ".pdf");
    }

    @GetMapping("/balance-sheet")
    public String balanceSheet(@RequestParam(required = false) LocalDate asOfDate, Model model) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        model.addAttribute("report", accountingService.generateBalanceSheet(date));
        model.addAttribute("asOfDate", date);
        return "reports/balance-sheet";
    }

    @GetMapping(value = "/balance-sheet/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> balanceSheetPdf(@RequestParam(required = false) LocalDate asOfDate,
                                                   @AuthenticationPrincipal CustomUserDetails principal) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        Map<String, Object> model = new HashMap<>();
        model.put("report", accountingService.generateBalanceSheet(date));
        model.put("asOfDate", date);
        addGenerationMeta(model, principal);
        byte[] pdf = pdfExportService.renderPdf("reports/pdf/balance-sheet-pdf", model);
        return pdfResponse(pdf, "balance-sheet-" + date + ".pdf");
    }

    @GetMapping("/income-statement")
    public String incomeStatement(@RequestParam(required = false) LocalDate fromDate,
                                   @RequestParam(required = false) LocalDate toDate,
                                   Model model) {
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : to.with(TemporalAdjusters.firstDayOfMonth());
        model.addAttribute("report", accountingService.generateIncomeStatement(from, to));
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        return "reports/income-statement";
    }

    @GetMapping(value = "/income-statement/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> incomeStatementPdf(@RequestParam(required = false) LocalDate fromDate,
                                                       @RequestParam(required = false) LocalDate toDate,
                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : to.with(TemporalAdjusters.firstDayOfMonth());
        Map<String, Object> model = new HashMap<>();
        model.put("report", accountingService.generateIncomeStatement(from, to));
        model.put("fromDate", from);
        model.put("toDate", to);
        addGenerationMeta(model, principal);
        byte[] pdf = pdfExportService.renderPdf("reports/pdf/income-statement-pdf", model);
        return pdfResponse(pdf, "income-expenditure-" + from + "-to-" + to + ".pdf");
    }

    @GetMapping("/general-ledger")
    public String generalLedger(@RequestParam(required = false) Long accountId,
                                 @RequestParam(required = false) LocalDate fromDate,
                                 @RequestParam(required = false) LocalDate toDate,
                                 Model model) {
        model.addAttribute("accounts", accountingService.findAllAccounts());
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : to.with(TemporalAdjusters.firstDayOfMonth());
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("selectedAccountId", accountId);
        if (accountId != null) {
            model.addAttribute("report", accountingService.generateGeneralLedger(accountId, from, to));
        }
        return "reports/general-ledger";
    }

    @GetMapping(value = "/general-ledger/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generalLedgerPdf(@RequestParam Long accountId,
                                                     @RequestParam(required = false) LocalDate fromDate,
                                                     @RequestParam(required = false) LocalDate toDate,
                                                     @AuthenticationPrincipal CustomUserDetails principal) {
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : to.with(TemporalAdjusters.firstDayOfMonth());
        Map<String, Object> model = new HashMap<>();
        var report = accountingService.generateGeneralLedger(accountId, from, to);
        model.put("report", report);
        model.put("fromDate", from);
        model.put("toDate", to);
        addGenerationMeta(model, principal);
        byte[] pdf = pdfExportService.renderPdf("reports/pdf/general-ledger-pdf", model);
        return pdfResponse(pdf, "general-ledger-" + report.getAccount().getCode() + ".pdf");
    }

    @GetMapping("/member-statement")
    public String memberStatement(@RequestParam(required = false) Long clientId,
                                   @RequestParam(required = false) LocalDate fromDate,
                                   @RequestParam(required = false) LocalDate toDate,
                                   Model model) {
        model.addAttribute("clients", clientService.findAll());
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : to.minusMonths(3);
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("selectedClientId", clientId);
        if (clientId != null) {
            model.addAttribute("report", memberStatementService.generate(clientId, from, to));
        }
        return "reports/member-statement";
    }

    @GetMapping(value = "/member-statement/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> memberStatementPdf(@RequestParam Long clientId,
                                                       @RequestParam(required = false) LocalDate fromDate,
                                                       @RequestParam(required = false) LocalDate toDate,
                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : to.minusMonths(3);
        Map<String, Object> model = new HashMap<>();
        var report = memberStatementService.generate(clientId, from, to);
        model.put("report", report);
        model.put("fromDate", from);
        model.put("toDate", to);
        addGenerationMeta(model, principal);
        byte[] pdf = pdfExportService.renderPdf("reports/pdf/member-statement-pdf", model);
        return pdfResponse(pdf, "member-statement-" + report.getClient().getClientNumber() + ".pdf");
    }

    private void addGenerationMeta(Map<String, Object> model, CustomUserDetails principal) {
        model.put("generatedAt", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        model.put("generatedBy", principal != null ? principal.getUsername() : "-");
    }

    /** inline (not attachment) so the browser opens the PDF itself rather than forcing a download. */
    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
