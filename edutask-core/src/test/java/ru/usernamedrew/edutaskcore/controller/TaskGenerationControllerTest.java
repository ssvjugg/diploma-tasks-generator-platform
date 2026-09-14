package ru.usernamedrew.edutaskcore.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationResponse;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationStatus;
import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;
import ru.usernamedrew.edutaskcore.service.TaskGenerationService;
import ru.usernamedrew.edutaskcore.service.TaskGenerationSseService;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.usernamedrew.edutaskcore.controller.ControllerTestFixtures.TIMESTAMP;
import static ru.usernamedrew.edutaskcore.controller.ControllerTestFixtures.jwtWithRoles;

@WebMvcTest(controllers = TaskGenerationController.class)
@Import(TestConfig.class)
class TaskGenerationControllerTest {
    private static final UUID GENERATION_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID TOPIC_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskGenerationService taskGenerationService;

    @MockitoBean
    private TaskGenerationSseService taskGenerationSseService;

    @Test
    void havingGenerationReturns_whenPostGeneration_thenReturnCorrectResponse() throws Exception {
        TaskGenerationCreateRequest request = new TaskGenerationCreateRequest(
            "Сгенерируй задачу на циклы",
            Set.of(TOPIC_ID),
            TaskDifficulty.EASY,
            "openai",
            "gpt-5.4-nano",
            BigDecimal.valueOf(0.3)
        );
        TaskGenerationResponse response = generationResponse(TaskGenerationStatus.QUEUED);

        when(taskGenerationService.createGeneration(eq(request), any(Jwt.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks/generations")
                .with(jwtWithRoles("TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.requestId").value(GENERATION_ID.toString()))
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.provider").value("openai"))
            .andExpect(jsonPath("$.model").value("gpt-5.4-nano"));

        verify(taskGenerationService).createGeneration(eq(request), any(Jwt.class));
    }

    @Test
    void havingBadRequest_whenPostGeneration_thenReturnStatusBadRequest() throws Exception {
        TaskGenerationCreateRequest request = new TaskGenerationCreateRequest(
            "",
            Set.of(TOPIC_ID),
            TaskDifficulty.EASY,
            "open ai",
            "   ",
            BigDecimal.valueOf(2.1)
        );

        mockMvc.perform(post("/api/v1/tasks/generations")
                .with(jwtWithRoles("TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.detail").value("Request validation failed"))
            .andExpect(jsonPath("$.errors").isArray());

        verifyNoInteractions(taskGenerationService, taskGenerationSseService);
    }

    @Test
    void havingStudentAuthorized_whenPostGeneration_thenReturnStatusForbidden() throws Exception {
        TaskGenerationCreateRequest request = new TaskGenerationCreateRequest(
            "Сгенерируй задачу",
            Set.of(TOPIC_ID),
            TaskDifficulty.EASY,
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/tasks/generations")
                .with(jwtWithRoles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(taskGenerationService, taskGenerationSseService);
    }

    @Test
    void havingGeneration_whenGetGenerationById_thenReturnCorrectResponse() throws Exception {
        when(taskGenerationService.getGeneration(eq(GENERATION_ID), any(Jwt.class)))
            .thenReturn(generationResponse(TaskGenerationStatus.PROCESSING));

        mockMvc.perform(get("/api/v1/tasks/generations/{id}", GENERATION_ID)
                .with(jwtWithRoles("TEACHER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requestId").value(GENERATION_ID.toString()))
            .andExpect(jsonPath("$.status").value("PROCESSING"));

        verify(taskGenerationService).getGeneration(eq(GENERATION_ID), any(Jwt.class));
    }

    @Test
    void havingStreamGenerations_whenGetGenerationsStream_thenSubscribesAndSynchronizesLatestState() throws Exception {
        TaskGenerationResponse initialState = generationResponse(TaskGenerationStatus.PROCESSING);
        TaskGenerationResponse latestState = generationResponse(TaskGenerationStatus.COMPLETED);
        SseEmitter emitter = new SseEmitter(1_000L);

        when(taskGenerationService.getGeneration(eq(GENERATION_ID), any(Jwt.class)))
            .thenReturn(initialState, latestState);
        when(taskGenerationSseService.subscribe(initialState)).thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/tasks/generations/{id}/stream", GENERATION_ID)
                .with(jwtWithRoles("TEACHER")))
            .andExpect(request().asyncStarted())
            .andReturn();

        emitter.complete();
        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());

        verify(taskGenerationSseService).subscribe(initialState);
        verify(taskGenerationSseService).synchronize(initialState, latestState);
    }

    @Test
    void havingUserNotAuthorized_whenGetGenerationById_thenReturnStatusUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/generations/{id}", GENERATION_ID))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskGenerationService, taskGenerationSseService);
    }

    private TaskGenerationResponse generationResponse(TaskGenerationStatus status) {
        return new TaskGenerationResponse(
            GENERATION_ID,
            status,
            "openai",
            "gpt-5.4-nano",
            null,
            status == TaskGenerationStatus.FAILED ? "Failed" : null,
            TIMESTAMP,
            TIMESTAMP
        );
    }
}
