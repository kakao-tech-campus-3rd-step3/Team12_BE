package unischedule.everytime.dto;

import unischedule.everytime.dto.external.EverytimeTimetableRawResponseDto.PrimaryTable;

public record TimetableDto(
        String year,
        String semester,
        String identifier
) {

    public static TimetableDto from(PrimaryTable primaryTable) {
        return new TimetableDto(
                String.valueOf(primaryTable.year()),
                primaryTable.semester(),
                primaryTable.identifier()
        );

    }
}
