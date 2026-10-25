package com.danver.messengerserver.models.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class PagingNavigation {

    private String field;
    private String additionalField;
    @Builder.Default
    private Direction direction = Direction.PAST;
    private String pageToken;
    @Builder.Default
    private Integer maxPageSize = 50;
    private String cursor;
}
