package com.destiny.club.web;

import com.destiny.club.domain.savings.SavingsProduct;
import com.destiny.club.service.SavingsProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/savings-products")
public class SavingsProductController {

    private final SavingsProductService savingsProductService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", savingsProductService.findAll());
        return "savings-products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("product", new SavingsProduct());
        return "savings-products/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", savingsProductService.getById(id));
        return "savings-products/form";
    }

    @PostMapping
    public String save(@ModelAttribute("product") SavingsProduct product, RedirectAttributes redirectAttributes) {
        savingsProductService.save(product);
        redirectAttributes.addFlashAttribute("successMessage", "Savings product saved");
        return "redirect:/savings-products";
    }
}
