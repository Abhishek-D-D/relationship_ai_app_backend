package com.couplespace.repository;

import com.couplespace.entity.Couple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoupleRepository extends JpaRepository<Couple, UUID> {

    Optional<Couple> findByInviteCode(String inviteCode);

    @Query("SELECT c FROM Couple c WHERE c.partner1.userId = :userId OR c.partner2.userId = :userId")
    Optional<Couple> findByPartnerId(@Param("userId") UUID userId);

    @Query("SELECT c.coupleId FROM Couple c WHERE c.partner2 IS NOT NULL")
    List<UUID> findAllActiveCoupleIds();

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM Couple c WHERE (c.partner1.userId = :userId OR c.partner2.userId = :userId)")
    boolean existsByPartnerId(@Param("userId") UUID userId);
}
