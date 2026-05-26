package com.noonyuu.prc.infra.mapper

import com.noonyuu.prc.domain.entity.User
import com.noonyuu.prc.domain.entity.UserId
import com.noonyuu.prc.infra.dto.UserDto

fun UserDto.toEntity(): User =
    User(
        id = UserId(id),
        name = name,
        email = email,
    )
