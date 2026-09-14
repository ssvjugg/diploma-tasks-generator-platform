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
import ru.usernamedrew.edutaskcommon.dto.submission.CodeSubmissionCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.submission.CodeSubmissionResponse;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeSubmissionStatus;
import ru.usernamedrew.edutaskcore.exception.ResourceNotFoundException;
import ru.usernamedrew.edutaskcore.service.CodeSubmissionService;
import ru.usernamedrew.edutaskcore.service.CodeSubmissionSseService;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
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

@WebMvcTest(controllers = CodeSubmissionController.class)
@Import(TestConfig.class)
class CodeSubmissionControllerTest {
    private static final UUID TASK_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SUBMISSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CodeSubmissionService codeSubmissionService;

    @MockitoBean
    private CodeSubmissionSseService codeSubmissionSseService;

    @Test
    void havingSubmissionRequest_whenPostSubmission_thenReturnCorrectResponse() throws Exception {
        CodeSubmissionCreateRequest request = new CodeSubmissionCreateRequest("java", "class Main {}");
        CodeSubmissionResponse response = submissionResponse(SUBMISSION_ID, JudgeSubmissionStatus.QUEUED);

        when(codeSubmissionService.createSubmission(eq(TASK_ID), eq(request), any(Jwt.class)))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/submissions", TASK_ID)
                .with(jwtWithRoles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.submissionId").value(SUBMISSION_ID.toString()))
            .andExpect(jsonPath("$.taskId").value(TASK_ID.toString()))
            .andExpect(jsonPath("$.language").value("java"))
            .andExpect(jsonPath("$.status").value("QUEUED"));

        verify(codeSubmissionService).createSubmission(eq(TASK_ID), eq(request), any(Jwt.class));
    }

    @Test
    void havingBadRequest_whenPostSubmission_thenReturnStatusBadRequest() throws Exception {
        CodeSubmissionCreateRequest request = new CodeSubmissionCreateRequest("java script", "");

        mockMvc.perform(post("/api/v1/tasks/{taskId}/submissions", TASK_ID)
                .with(jwtWithRoles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.detail").value("Request validation failed"))
            .andExpect(jsonPath("$.errors").isArray());

        verifyNoInteractions(codeSubmissionService, codeSubmissionSseService);
    }

    @Test
    void havingSubmission_whenGetSubmissionById_thenReturnCorrectResponse() throws Exception {
        when(codeSubmissionService.getSubmission(eq(SUBMISSION_ID), any(Jwt.class)))
            .thenReturn(submissionResponse(SUBMISSION_ID, JudgeSubmissionStatus.ACCEPTED));

        mockMvc.perform(get("/api/v1/submissions/{submissionId}", SUBMISSION_ID)
                .with(jwtWithRoles("STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.submissionId").value(SUBMISSION_ID.toString()))
            .andExpect(jsonPath("$.sourceCode").value("class Main {}"))
            .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(codeSubmissionService).getSubmission(eq(SUBMISSION_ID), any(Jwt.class));
    }

    @Test
    void havingNoSubmission_whenGetSubmissionById_thenReturnProblemDetail() throws Exception {
        when(codeSubmissionService.getSubmission(eq(SUBMISSION_ID), any(Jwt.class)))
            .thenThrow(new ResourceNotFoundException("Submission not found: " + SUBMISSION_ID));

        mockMvc.perform(get("/api/v1/submissions/{submissionId}", SUBMISSION_ID)
                .with(jwtWithRoles("STUDENT")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource not found"))
            .andExpect(jsonPath("$.detail").value(containsString(SUBMISSION_ID.toString())));
    }

    @Test
    void havingStreamSubmission_whenGetSubmissionsStream_thenSubscribesAndSynchronizesLatestState() throws Exception {
        CodeSubmissionResponse initialState = submissionResponse(SUBMISSION_ID, JudgeSubmissionStatus.QUEUED);
        CodeSubmissionResponse latestState = submissionResponse(SUBMISSION_ID, JudgeSubmissionStatus.ACCEPTED);
        SseEmitter emitter = new SseEmitter(1_000L);

        when(codeSubmissionService.getSubmission(eq(SUBMISSION_ID), any(Jwt.class), eq(false)))
            .thenReturn(initialState, latestState);
        when(codeSubmissionSseService.subscribe(initialState)).thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/submissions/{submissionId}/stream", SUBMISSION_ID)
                .with(jwtWithRoles("STUDENT")))
            .andExpect(request().asyncStarted())
            .andReturn();

        emitter.complete();
        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());

        verify(codeSubmissionSseService).subscribe(initialState);
        verify(codeSubmissionSseService).synchronize(initialState, latestState);
    }

    @Test
    void havingUserNotAuthorized_whenGetSubmissionById_thenReturnStatusUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/submissions/{submissionId}", SUBMISSION_ID))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(codeSubmissionService, codeSubmissionSseService);
    }

    private CodeSubmissionResponse submissionResponse(UUID submissionId, JudgeSubmissionStatus status) {
        return new CodeSubmissionResponse(
            submissionId,
            TASK_ID,
            USER_ID,
            "java",
            "class Main {}",
            status,
            List.of(),
            status == JudgeSubmissionStatus.ACCEPTED ? 1 : 0,
            1,
            status == JudgeSubmissionStatus.ACCEPTED ? 1 : 0,
            1,
            null,
            TIMESTAMP,
            TIMESTAMP
        );
    }
}
