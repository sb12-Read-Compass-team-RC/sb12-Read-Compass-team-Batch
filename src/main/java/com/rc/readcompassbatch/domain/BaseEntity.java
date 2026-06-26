package com.rc.readcompassbatch.domain;

import com.fasterxml.uuid.Generators;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 메인 프로젝트(read-compass)와 동일한 ID 생성 전략(UUID v7)을 공유한다.
 * createdAt 은 DB default(now())에도 의존하지만, 배치에서 직접 insert 하는
 * 스냅샷/알림은 영속 시점에 채워 둔다.
 */
@Getter
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {
    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = Generators.timeBasedEpochGenerator().generate();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
