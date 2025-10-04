package unischedule.common.dto;

public record PaginationRequestDto(
        int page,
        int limit,
        String search
) {
    public static PaginationRequestDto of(int page, int limit) {
        return new PaginationRequestDto(page, limit, null);
    }
}
