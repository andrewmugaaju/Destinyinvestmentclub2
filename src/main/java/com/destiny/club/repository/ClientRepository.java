package com.destiny.club.repository;

import com.destiny.club.domain.client.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByClientNumber(String clientNumber);
}
