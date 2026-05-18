package ru.usernamedrew.edutaskcore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.usernamedrew.edutaskcommon.dto.generation.TaskGenerationResponse;
import ru.usernamedrew.edutaskcore.entity.GenerationRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TaskGenerationMapper {
    @Mapping(target = "requestId", source = "id")
    @Mapping(target = "provider", source = "modelProvider")
    @Mapping(target = "model", source = "modelName")
    @Mapping(target = "result", source = "generatedDraft")
    TaskGenerationResponse toResponse(GenerationRequest generationRequest);
}
