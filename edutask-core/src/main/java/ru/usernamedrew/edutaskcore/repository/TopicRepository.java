package ru.usernamedrew.edutaskcore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicSummary;
import ru.usernamedrew.edutaskcore.entity.Topic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {
    // TODO Убрать лишние запросы
    @EntityGraph(attributePaths = "parent")
    @Query("select t from Topic t where t.id = :id")
    Optional<Topic> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "parent")
    @Query("""
        select t
        from Topic t
        """)
    Page<Topic> findAllDetailed(Pageable pageable);

    @EntityGraph(attributePaths = "parent")
    @Query("""
        select t
        from Topic t
        where lower(t.name) like :queryPattern
        """)
    Page<Topic> findByNameLike(@Param("queryPattern") String queryPattern, Pageable pageable);

    @EntityGraph(attributePaths = "parent")
    @Query("""
        select t
        from Topic t
        where t.parent is null
        """)
    Page<Topic> findByParentIsNull(Pageable pageable);

    @EntityGraph(attributePaths = "parent")
    @Query("""
        select t
        from Topic t
        where lower(t.name) like :queryPattern
          and t.parent is null
        """)
    Page<Topic> findByNameLikeAndParentIsNull(@Param("queryPattern") String queryPattern, Pageable pageable);

    @Query("""
        select new ru.usernamedrew.edutaskcommon.dto.topic.TopicSummary(t.id, t.name)
        from Topic t
        order by lower(t.name), t.id
        """)
    List<TopicSummary> findSummaries(Pageable pageable);

    @Query("""
        select new ru.usernamedrew.edutaskcommon.dto.topic.TopicSummary(t.id, t.name)
        from Topic t
        where lower(t.name) like :queryPattern
        order by lower(t.name), t.id
        """)
    List<TopicSummary> findSummariesByNameLike(@Param("queryPattern") String queryPattern, Pageable pageable);

    @EntityGraph(attributePaths = "parent")
    @Query("""
        select t
        from Topic t
        left join t.parent p
        where p.id = :parentId
        """)
    Page<Topic> findByParentId(@Param("parentId") UUID parentId, Pageable pageable);

    @EntityGraph(attributePaths = "parent")
    @Query("""
        select t
        from Topic t
        left join t.parent p
        where lower(t.name) like :queryPattern
          and p.id = :parentId
        """)
    Page<Topic> findByFilters(
        @Param("queryPattern") String queryPattern,
        @Param("parentId") UUID parentId,
        Pageable pageable
    );
}
