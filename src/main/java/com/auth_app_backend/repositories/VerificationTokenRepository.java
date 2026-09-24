package com.auth_app_backend.repositories;

import com.auth_app_backend.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByTokenAndType(String token, VerificationToken.TokenType type);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.user.id = :userId AND vt.type = :type")
    void deleteByUserAndType(@Param("userId") UUID userId,
                             @Param("type") VerificationToken.TokenType type);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}