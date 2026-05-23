package ru.usernamedrew.edutaskjudgeworker.judge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import ru.usernamedrew.edutaskjudgeworker.config.Judge0Properties;
import ru.usernamedrew.edutaskjudgeworker.exception.Judge0ClientException;
import ru.usernamedrew.edutaskjudgeworker.exception.Judge0PollingTimeoutException;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class Judge0Client {
    private static final String RESULT_FIELDS = "token,stdout,stderr,compile_output,message,status,time,memory";

    private final WebClient judge0WebClient;
    private final Judge0Properties properties;

    // Отправка решения в judge0, не batch отправка!
    public String createSubmission(Judge0SubmissionRequest request) {
        try {
            Judge0SubmissionToken response = judge0WebClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/submissions")
                    .queryParam("base64_encoded", false)
                    .queryParam("wait", false)
                    .build())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Judge0SubmissionToken.class)
                .block(properties.getRequestTimeout());

            if (response == null || response.token() == null || response.token().isBlank()) {
                throw new Judge0ClientException("Judge0 did not return submission token");
            }
            return response.token();
        } catch (WebClientException exception) {
            throw new Judge0ClientException("Failed to create Judge0 submission", exception);
        }
    }

    public Judge0SubmissionResult waitForResult(String token) {
        Instant deadline = Instant.now().plus(properties.getPollingTimeout());
        Judge0SubmissionResult latest = null;

        while (Instant.now().isBefore(deadline)) {
            latest = fetchSubmission(token);
            if (latest != null && latest.isFinished()) {
                return latest;
            }
            sleep(properties.getPollingInterval());
        }

        String status = latest == null || latest.status() == null ? "unknown" : latest.status().description();
        throw new Judge0PollingTimeoutException(
            "Judge0 polling timed out for token " + token + ", lastStatus=" + status
        );
    }

    private Judge0SubmissionResult fetchSubmission(String token) {
        try {
            return judge0WebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/submissions/{token}")
                    .queryParam("base64_encoded", false)
                    .queryParam("fields", RESULT_FIELDS)
                    .build(token))
                .retrieve()
                .bodyToMono(Judge0SubmissionResult.class)
                .block(properties.getRequestTimeout());
        } catch (WebClientException exception) {
            throw new Judge0ClientException("Failed to fetch Judge0 submission result", exception);
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new Judge0ClientException("Interrupted while polling Judge0 submission", exception);
        }
    }
}
