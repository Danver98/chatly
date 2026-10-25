package com.danver.messengerserver.models.util;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_VALUES)
public enum SortDirection {
    ASC,
    DESC;
}
