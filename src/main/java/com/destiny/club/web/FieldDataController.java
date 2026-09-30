package com.destiny.club.web;

import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.fielddata.FieldData;
import com.destiny.club.domain.savings.SavingsAccount;
import com.destiny.club.domain.savings.SavingsAccountStatus;
import com.destiny.club.security.CustomUserDetails;
import com.destiny.club.service.ClientService;
import com.destiny.club.service.FieldDataService;
import com.destiny.club.service.PdfExportService;
import com.destiny.club.service.SavingsProductService;
import com.destiny.club.service.SavingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Field Data screen: a field officer records a savings collection made out in the field.
 * Any officer can collect from any member, regardless of which permanent group the member
 * belongs to - the two-person collection team rotates daily/weekly and is just typed in as
 * plain names, not tied to a system account.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/field-data")
public class FieldDataController {

    private final FieldDataService fieldDataService;
    private final ClientService clientService;
    private final SavingsProductService savingsProductService;
    private final SavingsService savingsService;
    private final PdfExportService pdfExportService;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @GetMapping
    public String list(@RequestParam(required = false) LocalDate fromDate,
                        @RequestParam(required = false) LocalDate toDate,
                        Model model) {
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : to.with(TemporalAdjusters.firstDayOfMonth());
        List<FieldData> entries = fieldDataService.findBetween(from, to);
        model.addAttribute("entries", entries);
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("totalAmount", totalOf(entries));
        return "field-data/list";
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@RequestParam(required = false) LocalDate fromDate,
                                       @RequestParam(required = false) LocalDate toDate,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : to.with(TemporalAdjusters.firstDayOfMonth());
        List<FieldData> entries = fieldDataService.findBetween(from, to);
        Map<String, Object> model = new HashMap<>();
        model.put("entries", entries);
        model.put("fromDate", from);
        model.put("toDate", to);
        model.put("totalAmount", totalOf(entries));
        model.put("generatedAt", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        model.put("generatedBy", principal != null ? principal.getUsername() : "-");
        byte[] pdf = pdfExportService.renderPdf("reports/pdf/field-data-report-pdf", model);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"field-data-report-" + from + "-to-" + to + ".pdf\"")
                .body(pdf);
    }

    private BigDecimal totalOf(List<FieldData> entries) {
        return entries.stream().map(f -> f.getSavingsTransaction().getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long clientId, Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("savingsProducts", savingsProductService.findActive());
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("knownCollectors", fieldDataService.knownCollectorNames());

        String selectedMemberLabel = null;
        if (clientId != null) {
            Client client = clientService.getById(clientId);
            selectedMemberLabel = client.getFullName() + " (" + client.getClientNumber() + ")";
            List<SavingsAccount> existing = savingsService.findByClient(clientId).stream()
                    .filter(a -> a.getStatus() == SavingsAccountStatus.ACTIVE)
                    .toList();
            model.addAttribute("existingByProductId", existing.stream()
                    .collect(Collectors.toMap(a -> a.getSavingsProduct().getId(), a -> a, (a, b) -> a)));
        }
        model.addAttribute("selectedMemberLabel", selectedMemberLabel);
        return "field-data/form";
    }

    @PostMapping
    public String save(@RequestParam Long clientId,
                        @RequestParam Long savingsProductId,
                        @RequestParam BigDecimal amount,
                        @RequestParam(required = false) LocalDate transactionDate,
                        @RequestParam String collectorOneName,
                        @RequestParam(required = false) String collectorTwoName,
                        @RequestParam(required = false) String narration,
                        @AuthenticationPrincipal CustomUserDetails principal,
                        RedirectAttributes redirectAttributes) {
        Client client = clientService.getById(clientId);
        var product = savingsProductService.getById(savingsProductId);
        fieldDataService.record(client, product, amount, transactionDate, collectorOneName, collectorTwoName,
                narration, principal.getUsername());
        redirectAttributes.addFlashAttribute("successMessage",
                "Collected " + amount + " from " + client.getFullName() + " - ready for the next one");
        return "redirect:/field-data/new";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("entry", fieldDataService.getById(id));
        return "field-data/view";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String edit(@PathVariable Long id,
                        @RequestParam(required = false) String narration,
                        @AuthenticationPrincipal CustomUserDetails principal,
                        RedirectAttributes redirectAttributes) {
        fieldDataService.updateNarration(id, narration, principal.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Field collection updated");
        return "redirect:/field-data/" + id;
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String voidEntry(@PathVariable Long id,
                             @RequestParam(required = false) String reason,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             RedirectAttributes redirectAttributes) {
        fieldDataService.voidEntry(id, principal.getUsername(), reason);
        redirectAttributes.addFlashAttribute("successMessage", "Field collection voided");
        return "redirect:/field-data/" + id;
    }
}
