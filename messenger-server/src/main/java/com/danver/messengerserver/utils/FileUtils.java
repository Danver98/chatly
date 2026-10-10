package com.danver.messengerserver.utils;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Objects;

public class FileUtils {

    private static final String [] IMAGE_EXTENSIONS = {
            "jpg",
            "jpeg",
            "png",
            "gif"
    };

    private static final String [] AUDIO_EXTENSIONS = {
            "mp3",
            "ogg"
    };

    private static final String [] VIDEO_EXTENSIONS = {
            "mp4"
    };

    public static String getExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null) return null;
        int index = name.lastIndexOf('.');
        if (index == -1 || index == name.length() - 1) {
            return null;
        }
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public static boolean isImage(MultipartFile file) {
        String ext = FileUtils.getExtension(file);
        if (Arrays.asList(IMAGE_EXTENSIONS).contains(ext)) {
            return true;
        }
        if (Objects.equals(file.getContentType(), MediaType.IMAGE_JPEG_VALUE) ||
                Objects.equals(file.getContentType(), MediaType.IMAGE_PNG_VALUE) ||
                Objects.equals(file.getContentType(), MediaType.IMAGE_GIF_VALUE)) {
            return true;
        }
        return false;
    }

    public static boolean isAudio(MultipartFile file) {
        String ext = FileUtils.getExtension(file);
        if (Arrays.asList(AUDIO_EXTENSIONS).contains(ext)) {
            return true;
        }
        if (Objects.equals(file.getContentType(), "audio/mpeg") ||
                Objects.equals(file.getContentType(), "audio/ogg") ||
                Objects.equals(file.getContentType(), "audio/mp4")) {
            return true;
        }
        return false;
    }

    public static boolean isVideo(MultipartFile file) {
        String ext = FileUtils.getExtension(file);
        if (Arrays.asList(VIDEO_EXTENSIONS).contains(ext)) {
            return true;
        }
        if (Objects.equals(file.getContentType(), "video/mp4")) {
            return true;
        }
        return false;
    }

    public static boolean isValid(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && !name.isEmpty() &&
                file.getContentType() != null;
    }
}
