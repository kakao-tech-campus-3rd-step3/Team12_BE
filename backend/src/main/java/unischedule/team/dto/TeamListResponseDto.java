package unischedule.team.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import unischedule.common.dto.PaginationMetadataDto;

import java.util.List;

public record TeamListResponseDto(
        List<TeamResponseDto> items,

        @JsonProperty("pagination_metadata")
        PaginationMetadataDto pagination
) {
}
