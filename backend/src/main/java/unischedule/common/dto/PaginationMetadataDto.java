package unischedule.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

public record PaginationMetadataDto(
        int page,
        int limit,
        @JsonProperty("total_page")
        int totalPage,
        @JsonProperty("total_count")
        long totalCount
) {
    public static PaginationMetadataDto from(Page<?> pageData) {
        return new PaginationMetadataDto(
                pageData.getPageable().getPageNumber() + 1,
                pageData.getPageable().getPageSize(),
                pageData.getTotalPages(),
                pageData.getTotalElements()
        );
    }
}
