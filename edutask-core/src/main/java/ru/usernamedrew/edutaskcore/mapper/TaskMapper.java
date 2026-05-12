package ru.usernamedrew.edutaskcore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.usernamedrew.edutaskcommon.dto.task.ProgrammingLanguageSummary;
import ru.usernamedrew.edutaskcommon.dto.task.TaskResponse;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicSummary;
import ru.usernamedrew.edutaskcore.entity.ProgrammingLanguage;
import ru.usernamedrew.edutaskcore.entity.Task;
import ru.usernamedrew.edutaskcore.entity.Topic;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TaskMapper {
    @Mapping(target = "authorId", source = "author.id")
    TaskResponse toResponse(Task task);

    @Mapping(target = "parentId", source = "parent.id")
    TopicSummary toSummary(Topic topic);

    ProgrammingLanguageSummary toSummary(ProgrammingLanguage language);
}
