package ru.usernamedrew.edutaskcore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicResponse;
import ru.usernamedrew.edutaskcommon.dto.topic.TopicSummary;
import ru.usernamedrew.edutaskcore.entity.Topic;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TopicMapper {
    @Mapping(target = "parentId", source = "parent.id")
    TopicResponse toResponse(Topic topic);

    @Mapping(target = "parentId", source = "parent.id")
    TopicSummary toSummary(Topic topic);
}
