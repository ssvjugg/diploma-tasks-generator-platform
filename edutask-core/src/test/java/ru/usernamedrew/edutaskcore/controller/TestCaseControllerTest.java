package ru.usernamedrew.edutaskcore.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.usernamedrew.edutaskcommon.dto.testcase.TestCaseCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.testcase.TestCaseResponse;
import ru.usernamedrew.edutaskcommon.dto.testcase.TestCaseUpdateRequest;
import ru.usernamedrew.edutaskcore.service.TestCaseService;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.usernamedrew.edutaskcore.controller.ControllerTestFixtures.TIMESTAMP;
import static ru.usernamedrew.edutaskcore.controller.ControllerTestFixtures.jwtWithRoles;

@WebMvcTest(controllers = TestCaseController.class)
@Import(TestConfig.class)
class TestCaseControllerTest {
    private static final UUID TASK_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TEST_CASE_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TestCaseService testCaseService;

    @Test
    void havingTestCasesWithStudentAuthorized_whenGetTestCases_thenReturnCorrectResponseWithoutHiddenTests() throws Exception {
        when(testCaseService.getTaskTestCases(TASK_ID, false))
            .thenReturn(List.of(testCaseResponse(false, 1)));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/test-cases", TASK_ID)
                .with(jwtWithRoles("STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(TEST_CASE_ID.toString()))
            .andExpect(jsonPath("$[0].hidden").value(false));

        verify(testCaseService).getTaskTestCases(TASK_ID, false);
    }

    @Test
    void havingTestCasesWithTeacherAuthorized_whenGetTestCases_thenReturnCorrectResponseWithHiddenTests() throws Exception {
        when(testCaseService.getTaskTestCases(TASK_ID, true))
            .thenReturn(List.of(testCaseResponse(true, 2)));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/test-cases", TASK_ID)
                .with(jwtWithRoles("TEACHER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hidden").value(true))
            .andExpect(jsonPath("$[0].points").value(2));

        verify(testCaseService).getTaskTestCases(TASK_ID, true);
    }

    @Test
    void havingTestCase_whenPostTestCase_thenReturnCorrectResponse() throws Exception {
        TestCaseCreateRequest request = new TestCaseCreateRequest("2 3", "5", false, 1);
        when(testCaseService.createTestCase(TASK_ID, request)).thenReturn(testCaseResponse(false, 1));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/test-cases", TASK_ID)
                .with(jwtWithRoles("TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(TEST_CASE_ID.toString()))
            .andExpect(jsonPath("$.inputData").value("2 3"))
            .andExpect(jsonPath("$.expectedOutput").value("5"));

        verify(testCaseService).createTestCase(TASK_ID, request);
    }

    @Test
    void havingBadRequest_whenPostTestCase_thenReturnStatusBadRequest() throws Exception {
        TestCaseCreateRequest request = new TestCaseCreateRequest(null, null, false, -1);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/test-cases", TASK_ID)
                .with(jwtWithRoles("TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"));

        verifyNoInteractions(testCaseService);
    }

    @Test
    void havingStudentAuthorized_whenPostTestCase_thenReturnStatusForbidden() throws Exception {
        TestCaseCreateRequest request = new TestCaseCreateRequest("2 3", "5", false, 1);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/test-cases", TASK_ID)
                .with(jwtWithRoles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(testCaseService);
    }

    @Test
    void havingUpdatedTestCase_whenPatchTestCase_thenReturnCorrectResponse() throws Exception {
        TestCaseUpdateRequest request = new TestCaseUpdateRequest("4 5", "9", true, 2);
        when(testCaseService.patchTestCase(TASK_ID, TEST_CASE_ID, request))
            .thenReturn(testCaseResponse(true, 2));

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/test-cases/{testCaseId}", TASK_ID, TEST_CASE_ID)
                .with(jwtWithRoles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hidden").value(true))
            .andExpect(jsonPath("$.points").value(2));

        verify(testCaseService).patchTestCase(TASK_ID, TEST_CASE_ID, request);
    }

    @Test
    void havingTestCaseId_whenDeleteTestCase_thenReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/tasks/{taskId}/test-cases/{testCaseId}", TASK_ID, TEST_CASE_ID)
                .with(jwtWithRoles("ADMIN")))
            .andExpect(status().isNoContent());

        verify(testCaseService).deleteTestCase(TASK_ID, TEST_CASE_ID);
    }

    private TestCaseResponse testCaseResponse(boolean hidden, int points) {
        return new TestCaseResponse(
            TEST_CASE_ID,
            TASK_ID,
            "2 3",
            "5",
            hidden,
            points,
            TIMESTAMP,
            TIMESTAMP
        );
    }
}
