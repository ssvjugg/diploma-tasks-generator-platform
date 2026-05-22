package ru.usernamedrew.edutaskcore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.usernamedrew.edutaskcommon.dto.testcase.TestCaseCreateRequest;
import ru.usernamedrew.edutaskcommon.dto.testcase.TestCaseResponse;
import ru.usernamedrew.edutaskcommon.dto.testcase.TestCaseUpdateRequest;
import ru.usernamedrew.edutaskcore.entity.Task;
import ru.usernamedrew.edutaskcore.entity.TestCase;
import ru.usernamedrew.edutaskcore.exception.ResourceNotFoundException;
import ru.usernamedrew.edutaskcore.mapper.TestCaseMapper;
import ru.usernamedrew.edutaskcore.repository.TaskRepository;
import ru.usernamedrew.edutaskcore.repository.TestCaseRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestCaseService {
    private final TestCaseRepository testCaseRepository;
    private final TaskRepository taskRepository;
    private final TestCaseMapper testCaseMapper;

    @Transactional(readOnly = true)
    public List<TestCaseResponse> getTaskTestCases(UUID taskId, boolean includeHidden) {
        ensureTaskExists(taskId);
        List<TestCase> testCases = includeHidden
            ? testCaseRepository.findByTaskIdOrderByCreatedAtAscIdAsc(taskId)
            : testCaseRepository.findByTaskIdAndHiddenFalseOrderByCreatedAtAscIdAsc(taskId);

        return testCases.stream()
            .map(testCaseMapper::toResponse)
            .toList();
    }

    @Transactional
    public TestCaseResponse createTestCase(UUID taskId, TestCaseCreateRequest request) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        TestCase testCase = new TestCase();
        testCase.setTask(task);
        testCase.setInputData(request.inputData());
        testCase.setExpectedOutput(request.expectedOutput());
        testCase.setHidden(Boolean.TRUE.equals(request.hidden()));
        testCase.setPoints(request.points() == null ? 0 : request.points());

        TestCase savedTestCase = testCaseRepository.saveAndFlush(testCase);
        return testCaseMapper.toResponse(savedTestCase);
    }

    @Transactional
    public TestCaseResponse patchTestCase(UUID taskId, UUID testCaseId, TestCaseUpdateRequest request) {
        ensureTaskExists(taskId);
        TestCase testCase = findTaskTestCase(taskId, testCaseId);

        if (request.inputData() != null) {
            testCase.setInputData(request.inputData());
        }
        if (request.expectedOutput() != null) {
            testCase.setExpectedOutput(request.expectedOutput());
        }
        if (request.hidden() != null) {
            testCase.setHidden(request.hidden());
        }
        if (request.points() != null) {
            testCase.setPoints(request.points());
        }

        testCaseRepository.flush();
        return testCaseMapper.toResponse(testCase);
    }

    @Transactional
    public void deleteTestCase(UUID taskId, UUID testCaseId) {
        ensureTaskExists(taskId);
        TestCase testCase = findTaskTestCase(taskId, testCaseId);
        testCaseRepository.delete(testCase);
    }

    private void ensureTaskExists(UUID taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task not found: " + taskId);
        }
    }

    private TestCase findTaskTestCase(UUID taskId, UUID testCaseId) {
        return testCaseRepository.findByIdAndTaskId(testCaseId, taskId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "TestCase not found for task: " + testCaseId
            ));
    }
}
