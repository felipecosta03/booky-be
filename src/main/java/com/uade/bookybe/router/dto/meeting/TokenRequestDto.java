package com.uade.bookybe.router.dto.meeting;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenRequestDto {
    
    @NotBlank(message = "Reading club ID is required")
    private String readingClubId;

}
