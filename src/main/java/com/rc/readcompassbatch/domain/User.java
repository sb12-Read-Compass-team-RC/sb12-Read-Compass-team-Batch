package com.rc.readcompassbatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 물리 삭제 배치를 위한 최소 매핑. (논리 삭제 후 1일 경과한 유저를 정리)
 * 배치는 읽기/삭제만 하므로 닉네임/이메일 등은 매핑하지 않는다.
 */
@Entity
@Table(name = "tb_users")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
