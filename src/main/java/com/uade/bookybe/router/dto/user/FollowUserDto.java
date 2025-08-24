package com.uade.bookybe.router.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request to follow/unfollow a user")
public class FollowUserDto {
    
    @NotBlank(message = "Target user ID is required")
    @Schema(description = "ID of the user to follow/unfollow", example = "user-123", required = true)
    private String targetUserId;
}
