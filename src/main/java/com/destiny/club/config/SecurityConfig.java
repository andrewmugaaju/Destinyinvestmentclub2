package com.destiny.club.config;

import com.destiny.club.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /** Sends a Loan Officer straight to the Deposit Report on login - the only page they can use - and everyone else to the dashboard as before. */
    @Bean
    public AuthenticationSuccessHandler loanOfficerAwareSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isLoanOfficer = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_LOAN_OFFICER"));
            response.sendRedirect(isLoanOfficer ? "/deposits" : "/dashboard");
        };
    }

    /**
     * Every role except LOAN_OFFICER - used as the "everything else" set so a Loan Officer's
     * access stays limited to exactly what's explicitly granted below (the Deposit Screen),
     * rather than falling through to a broad "any authenticated user" default.
     */
    private static final String[] NON_LOAN_OFFICER_ROLES = {"ADMIN", "MANAGER", "ACCOUNTANT", "TELLER"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // /logout must be explicitly listed here, not just left to logout().permitAll() -
                        // that alone doesn't override the anyRequest().hasAnyRole(...) catch-all below,
                        // so a Loan Officer (who matches none of those roles) got a 403 on logging out.
                        .requestMatchers("/login", "/logout", "/css/**", "/js/**", "/webjars/**", "/error").permitAll()
                        .requestMatchers("/users/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/reports/member-statement/**", "/reports/member-statement",
                                "/reports/loans/**", "/reports/loans", "/reports/savings/**", "/reports/savings",
                                "/reports/savings-withdrawals/**", "/reports/savings-withdrawals").hasAnyRole(NON_LOAN_OFFICER_ROLES)
                        .requestMatchers("/reports/**").hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT")
                        .requestMatchers("/journal/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                        .requestMatchers("/gl-accounts/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                        .requestMatchers("/savings-products/**", "/loan-products/**").hasAnyRole("ADMIN", "MANAGER")
                        // A Loan Officer's one and only allowed area: the Deposit Screen.
                        .requestMatchers("/deposits/**").authenticated()
                        .anyRequest().hasAnyRole(NON_LOAN_OFFICER_ROLES)
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loanOfficerAwareSuccessHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
