package com.destiny.club.web;

import com.destiny.club.domain.accounting.GLAccount;
import com.destiny.club.exception.BusinessException;
import com.destiny.club.repository.GLAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/gl-accounts")
public class GLAccountController {

    private final GLAccountRepository glAccountRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("accounts", glAccountRepository.findAllByOrderByCode());
        return "gl-accounts/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("account", new GLAccount());
        return "gl-accounts/form";
    }

    @PostMapping
    public String save(@ModelAttribute("account") GLAccount account, RedirectAttributes redirectAttributes) {
        if (glAccountRepository.findByCode(account.getCode()).isPresent()) {
            throw new BusinessException("A GL account with code " + account.getCode() + " already exists");
        }
        account.setSystemAccount(false);
        glAccountRepository.save(account);
        redirectAttributes.addFlashAttribute("successMessage", "GL account created");
        return "redirect:/gl-accounts";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("account", glAccountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("GL account not found: " + id)));
        return "gl-accounts/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @RequestParam(required = false) Boolean cashAccount,
                          @RequestParam(required = false) Boolean active,
                          RedirectAttributes redirectAttributes) {
        GLAccount account = glAccountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("GL account not found: " + id));
        account.setCashAccount(Boolean.TRUE.equals(cashAccount));
        account.setActive(Boolean.TRUE.equals(active));
        glAccountRepository.save(account);
        redirectAttributes.addFlashAttribute("successMessage", "GL account updated");
        return "redirect:/gl-accounts";
    }
}
