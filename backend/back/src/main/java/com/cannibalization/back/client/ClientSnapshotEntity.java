package com.cannibalization.back.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_snapshot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientSnapshotEntity {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "as_of_at", nullable = false)
    private Instant asOfAt;
}