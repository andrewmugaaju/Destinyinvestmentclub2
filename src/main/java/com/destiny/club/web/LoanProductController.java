package com.destiny.club.web;

import com.destiny.club.domain.loan.LoanProduct;
import com.destiny.club.service.LoanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/loan-products")
public class LoanProductController {

    private final LoanProductService loanProductService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", loanProductService.findAll());
        return "loan-products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("product", new LoanProduct());
        return "loan-products/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", loanProductService.getById(id));
        return "loan-products/form";
    }

    @PostMapping
    public String save(@ModelAttribute("product") LoanProduct product, RedirectAttributes redirectAttributes) {
        loanProductService.save(product);
        redirectAttributes.addFlashAttribute("successMessage", "Loan product saved");
        return "redirect:/loan-products";
    }
}
