package ru.usernamedrew.edutaskcore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.usernamedrew.edutaskcommon.dto.testcase.TestCaseResponse;
import ru.usernamedrew.edutaskcore.entity.TestCase;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TestCaseMapper {
    @Mapping(target = "taskId", source = "task.id")
    TestCaseResponse toResponse(TestCase testCase);
}
