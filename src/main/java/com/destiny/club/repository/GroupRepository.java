package com.destiny.club.repository;

import com.destiny.club.domain.client.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
    boolean existsByGroupNumber(String groupNumber);
}
