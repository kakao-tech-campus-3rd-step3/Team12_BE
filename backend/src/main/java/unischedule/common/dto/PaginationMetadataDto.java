package unischedule.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaginationMetadataDto(
        int page,
        int limit,
        @JsonProperty("total_page")
        int totalPage,
        @JsonProperty("total_count")
        long totalCount
) {
}
