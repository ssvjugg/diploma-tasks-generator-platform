package ru.usernamedrew.edutaskcore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.usernamedrew.edutaskcommon.dto.user.UserProfileResponse;
import ru.usernamedrew.edutaskcore.entity.UserProfile;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserProfileMapper {
    UserProfileResponse toResponse(UserProfile userProfile);
}
