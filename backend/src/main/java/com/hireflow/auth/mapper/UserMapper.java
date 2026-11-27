package com.hireflow.auth.mapper;

import com.hireflow.auth.dto.CompanyUserResponse;
import com.hireflow.auth.dto.UserResponse;
import com.hireflow.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct generates the implementation at compile time (target/generated-sources). */
@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "invitationPending", expression = "java(user.getPasswordHash() == null)")
    CompanyUserResponse toCompanyUserResponse(User user);
}
