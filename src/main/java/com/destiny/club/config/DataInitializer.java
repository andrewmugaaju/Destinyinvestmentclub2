package com.destiny.club.config;

import com.destiny.club.domain.user.RoleName;
import com.destiny.club.repository.UserRepository;
import com.destiny.club.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Creates the first ADMIN user on application start-up if the users table is empty, using the
 * credentials configured under {@code app.default-admin.*}. Safe to run on every start: it only
 * acts when there are no users at all.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.default-admin.username}")
    private String defaultAdminUsername;

    @Value("${app.default-admin.password}")
    private String defaultAdminPassword;

    @Value("${app.default-admin.email}")
    private String defaultAdminEmail;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            userService.create(defaultAdminUsername, defaultAdminPassword, "System Administrator",
                    defaultAdminEmail, Set.of(RoleName.ADMIN));
            log.warn("Created default admin user '{}' - please log in and change the password immediately.", defaultAdminUsername);
        }
    }
}
