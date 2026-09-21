package com.destiny.club.service;

import com.destiny.club.domain.user.Role;
import com.destiny.club.domain.user.RoleName;
import com.destiny.club.domain.user.User;
import com.destiny.club.exception.BusinessException;
import com.destiny.club.exception.NotFoundException;
import com.destiny.club.repository.RoleRepository;
import com.destiny.club.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Transactional
    public User create(String username, String rawPassword, String fullName, String email, Set<RoleName> roleNames) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Username already taken: " + username);
        }
        if (roleNames == null || roleNames.isEmpty()) {
            throw new BusinessException("At least one role must be assigned");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setEnabled(true);
        user.setRoles(resolveRoles(roleNames));
        return userRepository.save(user);
    }

    @Transactional
    public User updateRolesAndStatus(Long userId, Set<RoleName> roleNames, boolean enabled) {
        User user = getById(userId);
        if (roleNames == null || roleNames.isEmpty()) {
            throw new BusinessException("At least one role must be assigned");
        }
        user.setRoles(resolveRoles(roleNames));
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    @Transactional
    public User resetPassword(Long userId, String newRawPassword) {
        User user = getById(userId);
        user.setPassword(passwordEncoder.encode(newRawPassword));
        return userRepository.save(user);
    }

    private Set<Role> resolveRoles(Set<RoleName> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (RoleName name : roleNames) {
            roles.add(roleRepository.findByName(name)
                    .orElseThrow(() -> new NotFoundException("Role not configured: " + name)));
        }
        return roles;
    }
}
