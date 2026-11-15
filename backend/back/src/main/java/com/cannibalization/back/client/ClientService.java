package com.cannibalization.back.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientSnapshotRepository repository;

    @Transactional(readOnly = true)
    public ClientCardResponse getCard(String clientId) {
        ClientSnapshotEntity snapshot = repository
                .findFirstByClientIdOrderByAsOfAtDesc(clientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Данные клиента не найдены"
                ));

        return new ClientCardResponse(
                snapshot.getClientId(),
                snapshot.getId(),
                snapshot.getAsOfAt()
        );
    }
}