package ru.usernamedrew.edutaskcore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.usernamedrew.edutaskcore.entity.Topic;

import java.util.Optional;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {
    @EntityGraph(attributePaths = "parent")
    @Query("select t from Topic t where t.id = :id")
    Optional<Topic> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "parent")
    @Query("""
        select t
        from Topic t
        left join t.parent p
        where (:query is null or lower(t.name) like concat('%', :query, '%'))
          and (:parentId is null or p.id = :parentId)
        """)
    Page<Topic> findByFilters(
        @Param("query") String query,
        @Param("parentId") UUID parentId,
        Pageable pageable
    );
}
