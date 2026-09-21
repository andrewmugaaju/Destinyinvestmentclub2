package com.destiny.club.repository;

import com.destiny.club.domain.user.Role;
import com.destiny.club.domain.user.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
