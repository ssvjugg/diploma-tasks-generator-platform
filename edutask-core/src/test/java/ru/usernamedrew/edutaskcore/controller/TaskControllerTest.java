package ru.usernamedrew.edutaskcore.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.usernamedrew.edutaskcommon.dto.task.TaskCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;
import ru.usernamedrew.edutaskcommon.dto.task.TaskResponse;
import ru.usernamedrew.edutaskcommon.dto.task.TaskSearchRequest;
import ru.usernamedrew.edutaskcommon.dto.task.TaskSummary;
import ru.usernamedrew.edutaskcommon.dto.task.TaskUpdateRequest;
import ru.usernamedrew.edutaskcommon.dto.testcase.TestCaseCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicSummary;
import ru.usernamedrew.edutaskcore.exception.ResourceNotFoundException;
import ru.usernamedrew.edutaskcore.service.TaskService;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(controllers = TaskController.class)
@Import(TestConfig.class)
class TaskControllerTest {
    private static final UUID TASK_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID AUTHOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID TOPIC_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    void havingTasksPage_whenGetTasks_thenReturnCorrectResponse() throws Exception {
        Page<TaskSummary> page = new PageImpl<>(List.of(taskSummary()), Pageable.ofSize(5), 1);
        when(taskService.findTasks(any(TaskSearchRequest.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/tasks")
                .with(jwtWithRoles("STUDENT"))
                .param("query", "array")
                .param("difficulty", "EASY")
                .param("authorId", AUTHOR_ID.toString())
                .param("topicId", TOPIC_ID.toString())
                .param("page", "1")
                .param("size", "5")
                .param("sort", "title,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(TASK_ID.toString()))
            .andExpect(jsonPath("$.content[0].title").value("Сумма двух чисел"))
            .andExpect(jsonPath("$.content[0].difficulty").value("EASY"));

        ArgumentCaptor<TaskSearchRequest> requestCaptor = ArgumentCaptor.forClass(TaskSearchRequest.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskService).findTasks(requestCaptor.capture(), pageableCaptor.capture());

        assertThat(requestCaptor.getValue())
            .isEqualTo(new TaskSearchRequest("array", TaskDifficulty.EASY, AUTHOR_ID, TOPIC_ID));
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("title").getDirection())
            .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void havingTasks_whenPostByQueryFilter_thenReturnCorrectResponse() throws Exception {
        TaskSearchRequest request = new TaskSearchRequest("loops", TaskDifficulty.MEDIUM, null, TOPIC_ID);
        when(taskService.findTasks(eq(request), any(Pageable.class)))
            .thenReturn(Page.empty());

        mockMvc.perform(post("/api/v1/tasks/search")
                .with(jwtWithRoles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());

        verify(taskService).findTasks(eq(request), any(Pageable.class));
    }

    @Test
    void havingTask_whenGetTaskById_thenReturnCorrectResponse() throws Exception {
        when(taskService.getTask(TASK_ID)).thenReturn(taskResponse("Сумма двух чисел"));

        mockMvc.perform(get("/api/v1/tasks/{id}", TASK_ID)
                .with(jwtWithRoles("STUDENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TASK_ID.toString()))
            .andExpect(jsonPath("$.title").value("Сумма двух чисел"))
            .andExpect(jsonPath("$.difficulty").value("EASY"))
            .andExpect(jsonPath("$.topics[0].id").value(TOPIC_ID.toString()));

        verify(taskService).getTask(TASK_ID);
    }

    @Test
    void havingNoTask_whenGetTaskById_thenReturnProblemDetail() throws Exception {
        when(taskService.getTask(TASK_ID)).thenThrow(new ResourceNotFoundException("Task not found: " + TASK_ID));

        mockMvc.perform(get("/api/v1/tasks/{id}", TASK_ID)
                .with(jwtWithRoles("STUDENT")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource not found"))
            .andExpect(jsonPath("$.detail").value(containsString(TASK_ID.toString())));
    }

    @Test
    void havingTask_whenPostTask_thenReturnSavedTask() throws Exception {
        TaskCreateRequest request = createRequest();
        when(taskService.createTask(eq(request), any(Jwt.class))).thenReturn(taskResponse("Сумма двух чисел"));

        mockMvc.perform(post("/api/v1/tasks")
                .with(jwtWithRoles("TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(TASK_ID.toString()))
            .andExpect(jsonPath("$.title").value("Сумма двух чисел"));

        verify(taskService).createTask(eq(request), any(Jwt.class));
    }

    @Test
    void havingBadRequest_whenPostTask_thenReturnStatusBadRequest() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest(
            "", "", null, null, null, Set.of(TOPIC_ID), List.of()
        );

        mockMvc.perform(post("/api/v1/tasks")
                .with(jwtWithRoles("TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.errors").isArray());

        verifyNoInteractions(taskService);
    }

    @Test
    void havingAuthorizedStudent_whenPostTask_thenReturnStatusForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                .with(jwtWithRoles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest())))
            .andExpect(status().isForbidden());

        verifyNoInteractions(taskService);
    }

    @Test
    void havingUpdatedTaskWithAdminAuthorized_whenPatchTask_thenReturnCorrectResponse() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest(
            "Обновленная задача",
            null,
            null,
            null,
            TaskDifficulty.MEDIUM,
            AUTHOR_ID,
            Set.of(TOPIC_ID)
        );
        when(taskService.patchTask(eq(TASK_ID), eq(request), eq(true)))
            .thenReturn(taskResponse("Обновленная задача"));

        mockMvc.perform(patch("/api/v1/tasks/{id}", TASK_ID)
                .with(jwtWithRoles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Обновленная задача"));

        verify(taskService).patchTask(TASK_ID, request, true);
    }

    @Test
    void havingUpdatedTaskWithTeacherAuthorized_whenPatchTask_thenReturnCorrectResponse() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest(
            "Обновленная задача",
            null,
            null,
            null,
            null,
            null,
            null
        );
        when(taskService.patchTask(eq(TASK_ID), eq(request), eq(false)))
            .thenReturn(taskResponse("Обновленная задача"));

        mockMvc.perform(patch("/api/v1/tasks/{id}", TASK_ID)
                .with(jwtWithRoles("TEACHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Обновленная задача"));

        verify(taskService).patchTask(TASK_ID, request, false);
    }

    @Test
    void havingTaskId_whenDeleteTask_thenReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/tasks/{id}", TASK_ID)
                .with(jwtWithRoles("ADMIN")))
            .andExpect(status().isNoContent());

        verify(taskService).deleteTask(TASK_ID);
    }

    private TaskCreateRequest createRequest() {
        return new TaskCreateRequest(
            "Сумма двух чисел",
            "Даны два числа. Выведите сумму.",
            "a b",
            "sum",
            TaskDifficulty.EASY,
            Set.of(TOPIC_ID),
            List.of(new TestCaseCreateRequest("2 3", "5", false, 1))
        );
    }

    private TaskResponse taskResponse(String title) {
        return new TaskResponse(
            TASK_ID,
            title,
            "Даны два числа. Выведите сумму.",
            "a b",
            "sum",
            TaskDifficulty.EASY,
            AUTHOR_ID,
            Set.of(new TopicSummary(TOPIC_ID, "Математика")),
            TIMESTAMP,
            TIMESTAMP
        );
    }

    private TaskSummary taskSummary() {
        return new TaskSummary(TASK_ID, "Сумма двух чисел", TaskDifficulty.EASY);
    }
}
