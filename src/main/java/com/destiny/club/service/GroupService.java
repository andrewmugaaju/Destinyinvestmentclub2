package com.destiny.club.service;

import com.destiny.club.domain.client.Client;
import com.destiny.club.domain.client.Group;
import com.destiny.club.exception.NotFoundException;
import com.destiny.club.repository.ClientRepository;
import com.destiny.club.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final ClientRepository clientRepository;

    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    public Group getById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Group not found: " + id));
    }

    public List<Client> members(Long groupId) {
        return clientRepository.findAll().stream()
                .filter(c -> c.getGroup() != null && c.getGroup().getId().equals(groupId))
                .toList();
    }

    @Transactional
    public Group save(Group group) {
        if (group.getId() == null && (group.getGroupNumber() == null || group.getGroupNumber().isBlank())) {
            group.setGroupNumber(generateGroupNumber());
        }
        return groupRepository.save(group);
    }

    private String generateGroupNumber() {
        long seq = groupRepository.count() + 1;
        String candidate;
        do {
            candidate = String.format("GRP%04d", seq++);
        } while (groupRepository.existsByGroupNumber(candidate));
        return candidate;
    }
}
