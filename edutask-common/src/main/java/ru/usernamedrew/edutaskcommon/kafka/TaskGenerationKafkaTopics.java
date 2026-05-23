package ru.usernamedrew.edutaskcommon.kafka;

public final class TaskGenerationKafkaTopics {
    private TaskGenerationKafkaTopics() {
    }

    public static String resolveDltTopic(TaskGenerationKafkaProperties properties, String sourceTopic) {
        if (properties.getTopics().getTaskGenerationRequests().equals(sourceTopic)) {
            return properties.getTopics().getTaskGenerationRequestsDlt();
        }
        if (properties.getTopics().getTaskGenerationResponses().equals(sourceTopic)) {
            return properties.getTopics().getTaskGenerationResponsesDlt();
        }
        if (properties.getTopics().getCodeSubmissionRequests().equals(sourceTopic)) {
            return properties.getTopics().getCodeSubmissionRequestsDlt();
        }
        if (properties.getTopics().getCodeSubmissionResults().equals(sourceTopic)) {
            return properties.getTopics().getCodeSubmissionResultsDlt();
        }
        return sourceTopic + ".dlt";
    }
}
