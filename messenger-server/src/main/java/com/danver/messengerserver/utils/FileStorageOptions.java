package com.danver.messengerserver.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class FileStorageOptions {
    private String fileName;
    /**
     * Relative path to file
     */
    private String path;
    private Long owner;
}
