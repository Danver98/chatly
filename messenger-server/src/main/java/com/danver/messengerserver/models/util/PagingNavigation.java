package com.danver.messengerserver.models.util;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class PagingNavigation {

    private String field;
    private String additionalField;
    @Builder.Default
    private Direction direction = Direction.PAST;
}
