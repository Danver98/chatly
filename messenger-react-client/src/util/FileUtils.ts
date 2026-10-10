import { MessageDataType } from "../models/Message";

const IMAGE_EXTENSIONS = [
    "jpg",
    "jpeg",
    "png",
    "gif"
];

const AUDIO_EXTENSIONS = [
    "mp3",
    "ogg"
];

const VIDEO_EXTENSIONS = [
    "mp4"
]

function getExtension(file: File | null): string | null {
    if (file == null) return null;
    const index = file.name.lastIndexOf('.');
    if (index === -1 || index === file.name.length - 1) {
        return null;
    };
    return file.name.substring(index + 1).toLowerCase();
}

/**
 * 
 * @param file
 * @returns MessageDataType for the file
 */
export function getType(file: File | null): number {
    const type = MessageDataType.FILE;
    const ext = getExtension(file);
    if (ext == null) return type;
    if (IMAGE_EXTENSIONS.includes(ext)) return MessageDataType.IMAGE;
    if (AUDIO_EXTENSIONS.includes(ext)) return MessageDataType.AUDIO;
    if (VIDEO_EXTENSIONS.includes(ext)) return MessageDataType.VIDEO;
    return type;
}
