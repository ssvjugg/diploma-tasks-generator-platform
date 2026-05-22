package ru.usernamedrew.edutaskcore.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.usernamedrew.edutaskcore.entity.GenerationRequest;

import java.util.Optional;
import java.util.UUID;

public interface GenerationRequestRepository extends JpaRepository<GenerationRequest, UUID> {
    @EntityGraph(attributePaths = "user")
    @Query("select gr from GenerationRequest gr where gr.id = :id")
    Optional<GenerationRequest> findDetailedById(@Param("id") UUID id);
}
