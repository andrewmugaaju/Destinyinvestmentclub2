package com.destiny.club.service;

import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.client.Group;
import com.destiny.club.exception.NotFoundException;
import com.destiny.club.repository.ClientRepository;
import com.destiny.club.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final GroupRepository groupRepository;

    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    public Client getById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found: " + id));
    }

    @Transactional
    public Client save(Client client, Long groupId) {
        if (groupId != null) {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
            client.setGroup(group);
        } else {
            client.setGroup(null);
        }

        if (client.getId() == null) {
            if (client.getClientNumber() == null || client.getClientNumber().isBlank()) {
                client.setClientNumber(generateClientNumber());
            }
            if (client.getDateJoined() == null) {
                client.setDateJoined(LocalDate.now());
            }
        }

        return clientRepository.save(client);
    }

    private String generateClientNumber() {
        long seq = clientRepository.count() + 1;
        String candidate;
        do {
            candidate = String.format("CL%05d", seq++);
        } while (clientRepository.existsByClientNumber(candidate));
        return candidate;
    }
}
