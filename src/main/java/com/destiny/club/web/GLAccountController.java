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
}
