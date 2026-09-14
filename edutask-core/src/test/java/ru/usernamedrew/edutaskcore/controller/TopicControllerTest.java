package ru.usernamedrew.edutaskcore.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicResponse;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicSearchRequest;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicSummary;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicUpdateRequest;
import ru.usernamedrew.edutaskcore.exception.ResourceNotFoundException;
import ru.usernamedrew.edutaskcore.service.TopicService;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.usernamedrew.edutaskcore.controller.ControllerTestFixtures.jwtWithRoles;

@WebMvcTest(controllers = TopicController.class)
@Import(TestConfig.class)
class TopicControllerTest {
    private static final UUID TOPIC_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID PARENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TopicService topicService;

    @Test
    void havingTopicsPage_whenGetTopics_thenReturnCorrectResponse() throws Exception {
        Page<TopicResponse> page = new PageImpl<>(List.of(
            topicResponse("Динамическое программирование"),
            topicResponse("Динамические массивы")
        ), Pageable.ofSize(5), 2);
        when(topicService.findTopics(any(TopicSearchRequest.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/topics")
                .with(jwtWithRoles("STUDENT"))
                .param("query", "динами")
                .param("parentId", PARENT_ID.toString())
                .param("rootOnly", "true")
                .param("page", "1")
                .param("size", "5")
                .param("sort", "name,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(TOPIC_ID.toString()))
            .andExpect(jsonPath("$.content[0].name").value("Динамическое программирование"))
            .andExpect(jsonPath("$.content[1].id").value(TOPIC_ID.toString()))
            .andExpect(jsonPath("$.content[1].name").value("Динамические массивы"));

        ArgumentCaptor<TopicSearchRequest> requestCaptor = ArgumentCaptor.forClass(TopicSearchRequest.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(topicService).findTopics(requestCaptor.capture(), pageableCaptor.capture());

        assertThat(requestCaptor.getValue()).isEqualTo(new TopicSearchRequest("динами", PARENT_ID, true));
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void havingTopics_whenGetByQuery_thenReturnCorrectResponse() throws Exception {
        when(topicService.searchTopics("дина", 7))
            .thenReturn(List.of(new TopicSummary(TOPIC_ID, "Динамическое программирование")));

        mockMvc.perform(get("/api/v1/topics/search")
                .with(jwtWithRoles("STUDENT"))
                .param("query", "дина")
                .param("limit", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(TOPIC_ID.toString()))
            .andExpect(jsonPath("$[0].name").value("Динамическое программирование"));

        verify(topicService).searchTopics("дина", 7);
    }

    @Test
    void havingBadRequest_whenGetTopics_thenReturnStatusBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/topics/search")
                .with(jwtWithRoles("STUDENT"))
                .param("limit", "0"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(topicService);
    }

    @Test
    void havingTopic_whenGetTopicById_thenReturnCorrectResponse() throws Exception {
        when(topicService.getTopic(TOPIC_ID)).thenReturn(topicResponse("Динамическое программирование"));

        mockMvc.perform(get("/api/v1/topics/{id}", TOPIC_ID)
                .with(jwtWithRoles("STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TOPIC_ID.toString()))
            .andExpect(jsonPath("$.parentId").value(PARENT_ID.toString()));

        verify(topicService).getTopic(TOPIC_ID);
    }

    @Test
    void havingNoTopic_whenGetTopicById_thenReturnProblemDetail() throws Exception {
        when(topicService.getTopic(TOPIC_ID)).thenThrow(new ResourceNotFoundException("Topic not found: " + TOPIC_ID));

        mockMvc.perform(get("/api/v1/topics/{id}", TOPIC_ID)
                .with(jwtWithRoles("STUDENT")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource not found"))
            .andExpect(jsonPath("$.detail").value(containsString(TOPIC_ID.toString())));
    }

    @Test
    void havingTopic_whenPostTopic_thenReturnSavedTopic() throws Exception {
        TopicCreateRequest request = new TopicCreateRequest("Динамическое программирование", PARENT_ID);
        when(topicService.createTopic(request)).thenReturn(topicResponse("Динамическое программирование"));

        mockMvc.perform(post("/api/v1/topics")
                .with(jwtWithRoles("TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(TOPIC_ID.toString()))
            .andExpect(jsonPath("$.name").value("Динамическое программирование"));

        verify(topicService).createTopic(request);
    }

    @Test
    void havingUpdatedTopic_whenPatchTopic_thenReturnCorrectResponse() throws Exception {
        TopicUpdateRequest request = new TopicUpdateRequest("Графы", PARENT_ID);
        when(topicService.patchTopic(TOPIC_ID, request)).thenReturn(topicResponse("Графы"));

        mockMvc.perform(patch("/api/v1/topics/{id}", TOPIC_ID)
                .with(jwtWithRoles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TOPIC_ID.toString()))
            .andExpect(jsonPath("$.name").value("Графы"));

        verify(topicService).patchTopic(TOPIC_ID, request);
    }

    @Test
    void havingTopicId_whenDeleteTopic_thenReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/topics/{id}", TOPIC_ID)
                .with(jwtWithRoles("ADMIN")))
            .andExpect(status().isNoContent());

        verify(topicService).deleteTopic(TOPIC_ID);
    }

    @Test
    void havingInvalidTopic_whenPostTopic_thenReturnStatusBadRequest() throws Exception {
        TopicCreateRequest request = new TopicCreateRequest("", PARENT_ID);

        mockMvc.perform(post("/api/v1/topics")
                .with(jwtWithRoles("TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"));

        verifyNoInteractions(topicService);
    }

    @Test
    void havingAuthorizedStudent_whenPostTopic_thenReturnStatusForbidden() throws Exception {
        TopicCreateRequest request = new TopicCreateRequest("Динамическое программирование", PARENT_ID);

        mockMvc.perform(post("/api/v1/topics")
                .with(jwtWithRoles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(topicService);
    }

    private TopicResponse topicResponse(String name) {
        return new TopicResponse(TOPIC_ID, name, PARENT_ID);
    }
}
