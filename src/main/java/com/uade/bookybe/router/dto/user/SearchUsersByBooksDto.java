package com.uade.bookybe.router.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
@Schema(description = "Request to search users by book list")
public class SearchUsersByBooksDto {
    
    @JsonProperty("book_ids")
    @NotEmpty(message = "Book list cannot be empty")
    @Size(max = 10, message = "Maximum 10 books allowed per search")
    @Schema(description = "List of book IDs to search for", example = "[\"book-001\", \"book-002\"]", required = true)
    private List<String> bookIds;
}
