package com.uade.bookybe.router.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Search criteria for users by geographic location")
public class SearchUsersByLocationDto {

  @NotNull(message = "Bottom left latitude is required")
  @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
  @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
  @Schema(description = "Bottom left latitude coordinate", example = "-34.6037", required = true)
  private Double bottomLeftLatitude;

  @NotNull(message = "Bottom left longitude is required")
  @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
  @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
  @Schema(description = "Bottom left longitude coordinate", example = "-58.3816", required = true)
  private Double bottomLeftLongitude;

  @NotNull(message = "Top right latitude is required")
  @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
  @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
  @Schema(description = "Top right latitude coordinate", example = "-34.5707", required = true)
  private Double topRightLatitude;

  @NotNull(message = "Top right longitude is required")
  @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
  @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
  @Schema(description = "Top right longitude coordinate", example = "-58.3426", required = true)
  private Double topRightLongitude;
}
