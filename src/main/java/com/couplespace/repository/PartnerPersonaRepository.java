package com.couplespace.repository;

import com.couplespace.entity.PartnerPersona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartnerPersonaRepository extends JpaRepository<PartnerPersona, UUID> {
    Optional<PartnerPersona> findByUserId(UUID userId);

    List<PartnerPersona> findByCoupleId(UUID coupleId);

    void deleteByUserId(UUID userId);
}
