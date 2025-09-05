package unischedule.everytime.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

/**
 * 에브리타임 서버 측 시간표 API 응답 DTO
 * @param table 시간표 정보
 * @param user 사용자 정보
 * @param primaryTables 기본 시간표 목록
 */
@JacksonXmlRootElement(localName = "response")
@JsonIgnoreProperties(ignoreUnknown = true)
public record EverytimeTimetableRawResponseDto (
        Table table,
        @JacksonXmlProperty(localName = "user") User user,
        @JacksonXmlProperty(localName = "primaryTables") PrimaryTables primaryTables
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Table(
            @JacksonXmlProperty(isAttribute = true) String year,
            @JacksonXmlProperty(isAttribute = true) String semester,
            @JacksonXmlProperty(isAttribute = true) String status,
            @JacksonXmlProperty(isAttribute = true) String identifier,

            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "subject")
            List<Subject> subject
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Subject(
            @JacksonXmlProperty(isAttribute = true) String id,

            Attr internal,
            Attr name,
            Attr professor,
            Time time,
            Attr place,
            Attr credit,
            Attr closed
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Time(
            @JacksonXmlProperty(isAttribute = true) String value,

            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "data")
            List<TimeData> data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimeData(
            @JacksonXmlProperty(isAttribute = true) int day,        // 0=월 ~ 6=일
            @JacksonXmlProperty(isAttribute = true) int starttime,  // 5분 단위 (ex: 108 = 09:00)
            @JacksonXmlProperty(isAttribute = true) int endtime,    // 123 = 10:15
            @JacksonXmlProperty(isAttribute = true) String place
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Attr(
            @JacksonXmlProperty(isAttribute = true, localName = "value")
            String value
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(
            @JacksonXmlProperty(isAttribute = true) String name,
            @JacksonXmlProperty(isAttribute = true) int isMine,
            @JacksonXmlProperty(isAttribute = true) int isFriend
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrimaryTables(
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "primaryTable")
            List<PrimaryTable> primaryTable
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrimaryTable(
            @JacksonXmlProperty(isAttribute = true) String year,
            @JacksonXmlProperty(isAttribute = true) String semester,
            @JacksonXmlProperty(isAttribute = true) String identifier
    ) {}
}
