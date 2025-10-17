package unischedule.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponseDto<T>(
        List<T> content,
        int page,
        int size,
        @JsonProperty("total_elements")
        long totalElements,
        @JsonProperty("total_pages")
        int totalPages
) {
    public static <T> PageResponseDto<T> from(Page<T> page) {
        return new PageResponseDto<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
