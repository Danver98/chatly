package com.danver.messengerserver.services.interfaces;

import com.danver.messengerserver.exceptions.StorageException;
import com.danver.messengerserver.utils.FileStorageOptions;
import org.springframework.web.multipart.MultipartFile;


public interface StorageService {

    /**
     *
     * @param file
     * @return path to resource
     * @throws StorageException
     */
    public String store(MultipartFile file) throws StorageException;

    /**
     *
     * @param file
     * @param options
     * @return path to resource
     * @throws StorageException
     */
    default public String store(MultipartFile file, FileStorageOptions options) throws StorageException {
        return null;
    };

    public void load(String filename);

    /*
        Returns root path where all files are stored
     */
    default public String getRootPath() {
        return null;
    };
}
