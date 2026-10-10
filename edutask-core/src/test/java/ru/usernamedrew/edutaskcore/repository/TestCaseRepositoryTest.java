package ru.usernamedrew.edutaskcore.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;
import ru.usernamedrew.edutaskcore.entity.Task;
import ru.usernamedrew.edutaskcore.entity.TestCase;
import ru.usernamedrew.edutaskcore.entity.UserProfile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCaseRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private TestCaseRepository testCaseRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;

    private Task task;

    @BeforeEach
    public void setUp() {
        UserProfile userProfile = new UserProfile();
        userProfile.setKeycloakId("id");
        userProfile.setRole(UserProfile.UserRole.STUDENT);
        userProfileRepository.saveAndFlush(userProfile);

        task = new Task();
        task.setTitle("title");
        task.setStatement("statement");
        task.setInputFormat("");
        task.setOutputFormat("");
        task.setDifficulty(TaskDifficulty.EASY);
        task.setAuthor(userProfile);
        taskRepository.saveAndFlush(task);

        TestCase testCase1 = new TestCase();
        testCase1.setTask(task);
        testCase1.setInputData("input1");
        testCase1.setExpectedOutput("output1");
        TestCase testCase2 = new TestCase();
        testCase2.setTask(task);
        testCase2.setInputData("input2");
        testCase2.setExpectedOutput("output2");
        testCase2.setHidden(true);

        testCaseRepository.saveAndFlush(testCase1);
        testCaseRepository.saveAndFlush(testCase2);
    }

    @Test
    void havingTestCases_whenSave_thenSuccessfullyFindByTaskIdOrderByCreatedAtAscIdAsc() {
        List<TestCase> testCases = testCaseRepository.findByTaskIdOrderByCreatedAtAscIdAsc(task.getId());

        assertEquals(2, testCases.size());
    }

    @Test
    void havingTestCases_whenSave_thenSuccessfullyFindByTaskIdAndHiddenFalseOrderByCreatedAtAscIdAsc() {
        List<TestCase> testCases = testCaseRepository.findByTaskIdAndHiddenFalseOrderByCreatedAtAscIdAsc(task.getId());

        assertEquals(1, testCases.size());
    }
}
