package unischedule.external.dto;

import java.util.List;

public record OpenAiChatCompletionResponseDto(
        List<Choice> choices
) {

    public record Choice(
            Message message
    ) {

    }

    public record Message(
            String role,
            String content
    ) {

    }
}
