package com.rc.readcompassbatch.repository;

import com.rc.readcompassbatch.domain.User;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** 물리 삭제 대상(논리 삭제 후 cutoff 경과) 건수 — 메트릭/로그용. */
    @Query(value = "SELECT COUNT(*) FROM tb_users WHERE is_deleted = true AND deleted_at < :cutoff",
        nativeQuery = true)
    int countHardDeletable(@Param("cutoff") Instant cutoff);

    /**
     * 논리 삭제 후 cutoff 이전인 유저를 물리 삭제한다.
     * 연관 데이터(리뷰/댓글/좋아요/알림 등)는 FK ON DELETE CASCADE 로 함께 삭제된다.
     * 반환: 삭제된 행 수
     */
    @Modifying
    @Query(value = "DELETE FROM tb_users WHERE is_deleted = true AND deleted_at < :cutoff",
        nativeQuery = true)
    int deleteSoftDeletedBefore(@Param("cutoff") Instant cutoff);
}
