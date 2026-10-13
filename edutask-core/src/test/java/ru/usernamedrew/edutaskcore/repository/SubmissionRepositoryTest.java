package ru.usernamedrew.edutaskcore.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.usernamedrew.edutaskcommon.dto.task.TaskDifficulty;
import ru.usernamedrew.edutaskcommon.event.judge.JudgeSubmissionStatus;
import ru.usernamedrew.edutaskcore.entity.ProgrammingLanguage;
import ru.usernamedrew.edutaskcore.entity.Submission;
import ru.usernamedrew.edutaskcore.entity.Task;
import ru.usernamedrew.edutaskcore.entity.UserProfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SubmissionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Test
    void havingSubmission_whenSave_thenSuccessfullyFindDetailedById() {
        UserProfile userProfile = new UserProfile();
        userProfile.setKeycloakId("id");
        userProfile.setRole(UserProfile.UserRole.STUDENT);
        entityManager.persist(userProfile);

        Task task = new Task();
        task.setTitle("title");
        task.setStatement("statement");
        task.setInputFormat("");
        task.setOutputFormat("");
        task.setDifficulty(TaskDifficulty.EASY);
        task.setAuthor(userProfile);
        entityManager.persist(task);

        ProgrammingLanguage programmingLanguage = new ProgrammingLanguage();
        programmingLanguage.setName("Golang");
        programmingLanguage.setCode("0");
        programmingLanguage.setJudge0LanguageId(0);
        entityManager.persist(programmingLanguage);

        Submission submission = new Submission();
        submission.setUser(userProfile);
        submission.setTask(task);
        submission.setLanguage(programmingLanguage);
        submission.setSourceCode("""
            func main() {
                fmt.Println("Hello, World!")
            }
            """);
        submission.setStatus(JudgeSubmissionStatus.QUEUED);
        submissionRepository.saveAndFlush(submission);

        entityManager.clear();

        Submission found = submissionRepository.findDetailedById(submission.getId()).get();

        assertEquals("id", found.getUser().getKeycloakId());
        assertEquals("title", found.getTask().getTitle());
        assertEquals("statement", found.getTask().getStatement());
        assertEquals("Golang", found.getLanguage().getName());
        assertEquals(0, found.getScore());
        assertEquals(0, found.getMaxScore());
        assertEquals(0, found.getPassedTests());
        assertEquals(0, found.getTotalTests());
        assertNull(found.getErrorMessage());
    }
}
