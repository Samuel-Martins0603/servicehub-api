package br.com.servicehub.api.dto;

import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.User;

import java.time.LocalDateTime;

/** Saida de usuario. Nunca contem password nem passwordHash (RN02). */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        String phone,
        String bio,
        String avatarUrl,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getPhone(),
                u.getBio(), u.getAvatarUrl(), u.isActive(), u.getCreatedAt(), u.getUpdatedAt());
    }
}
