package ru.usernamedrew.edutaskcore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicResponse;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicSearchRequest;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicSummary;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicUpdateRequest;
import ru.usernamedrew.edutaskcore.entity.Topic;
import ru.usernamedrew.edutaskcore.exception.BadRequestException;
import ru.usernamedrew.edutaskcore.exception.ResourceNotFoundException;
import ru.usernamedrew.edutaskcore.mapper.TopicMapper;
import ru.usernamedrew.edutaskcore.repository.TopicRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicService {
    private static final int MAX_SEARCH_LIMIT = 50;

    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;

    @Transactional(readOnly = true)
    public Page<TopicResponse> findTopics(TopicSearchRequest request, Pageable pageable) {
        return findTopics(normalizeQuery(request.query()), request.parentId(), Boolean.TRUE.equals(request.rootOnly()), pageable)
            .map(topicMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TopicSummary> searchTopics(String query, int limit) {
        String normalizedQuery = normalizeQuery(query);
        Pageable pageable = PageRequest.of(0, Math.min(limit, MAX_SEARCH_LIMIT));

        if (normalizedQuery == null) {
            return topicRepository.findSummaries(pageable);
        }
        return topicRepository.findSummariesByNameLike("%" + normalizedQuery + "%", pageable);
    }

    @Transactional(readOnly = true)
    public TopicResponse getTopic(UUID id) {
        return topicMapper.toResponse(findDetailedTopic(id));
    }

    @Transactional
    public TopicResponse createTopic(TopicCreateRequest request) {
        Topic topic = new Topic();
        topic.setName(request.name().trim());
        topic.setParent(resolveParent(request.parentId()));

        Topic savedTopic = topicRepository.saveAndFlush(topic);
        return topicMapper.toResponse(savedTopic);
    }

    @Transactional
    public TopicResponse patchTopic(UUID id, TopicUpdateRequest request) {
        Topic topic = findDetailedTopic(id);

        if (request.name() != null) {
            topic.setName(request.name().trim());
        }
        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new BadRequestException("Topic cannot be its own parent");
            }
            topic.setParent(resolveParent(request.parentId()));
        }

        topicRepository.flush();
        return topicMapper.toResponse(topic);
    }

    @Transactional
    public void deleteTopic(UUID id) {
        Topic topic = topicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
        topicRepository.delete(topic);
    }

    @Transactional(readOnly = true)
    public Set<Topic> resolveTopics(Set<UUID> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Topic> topics = new HashSet<>(topicRepository.findAllById(topicIds));
        if (topics.size() != topicIds.size()) {
            throw new ResourceNotFoundException("One or more topics were not found");
        }
        return topics;
    }

    private Topic findDetailedTopic(UUID id) {
        return topicRepository.findDetailedById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
    }

    private Topic resolveParent(UUID parentId) {
        if (parentId == null) {
            return null;
        }
        return topicRepository.findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent topic not found: " + parentId));
    }

    private Page<Topic> findTopics(String query, UUID parentId, boolean rootOnly, Pageable pageable) {
        if (parentId != null) {
            if (query == null) {
                return topicRepository.findByParentId(parentId, pageable);
            }
            return topicRepository.findByFilters("%" + query + "%", parentId, pageable);
        }

        if (rootOnly) {
            if (query == null) {
                return topicRepository.findByParentIsNull(pageable);
            }
            return topicRepository.findByNameLikeAndParentIsNull("%" + query + "%", pageable);
        }

        if (query == null) {
            return topicRepository.findAllDetailed(pageable);
        }

        return topicRepository.findByNameLike("%" + query + "%", pageable);
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim().toLowerCase();
    }
}
