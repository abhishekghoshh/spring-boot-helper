package io.github.abhishekghosh.user.service;

import io.github.abhishekghosh.user.dto.UserDTO;
import io.github.abhishekghosh.user.model.User;

public interface UserServiceI {
    default User toUser(UserDTO userDTO) {
        return User.builder()
                .id(userDTO.id())
                .name(userDTO.name())
                .email(userDTO.email())
                .build();
    }

    default UserDTO toUserDTO(User user) {
        return new UserDTO(user.getId(), user.getName(), user.getEmail());
    }
}
