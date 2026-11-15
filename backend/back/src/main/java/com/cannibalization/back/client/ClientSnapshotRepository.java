package com.cannibalization.back.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientSnapshotRepository
        extends JpaRepository<ClientSnapshotEntity, UUID> {

    Optional<ClientSnapshotEntity> findFirstByClientIdOrderByAsOfAtDesc(
            String clientId
    );
}