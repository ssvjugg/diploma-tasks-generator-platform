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
        return sourceTopic + ".dlt";
    }
}
