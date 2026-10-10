package com.danver.messengerserver.services.implementations;

import com.danver.messengerserver.exceptions.StorageException;
import com.danver.messengerserver.services.interfaces.StorageService;
import com.danver.messengerserver.utils.FileStorageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Qualifier("s3Storage")
public class S3Storage implements StorageService {
    private final S3Client s3;
    private final String bucketName;
    private final String endpoint;

    @Autowired
    public S3Storage(S3Client s3, Environment env) {
        this.s3 = s3;
        this.bucketName = env.getProperty("s3.storage.bucket");
        this.endpoint = env.getProperty("s3.storage.endpoint");
    }

    @Override
    public String store(MultipartFile file) throws StorageException {
        return null;
    }

    @Override
    public String store(MultipartFile file, FileStorageOptions options) throws StorageException {
        try {
            if (!bucketExists(bucketName)) {
                createBucket(bucketName);
            }

            String safeName = UUID.randomUUID() + "-" +
                    Paths.get(Objects.requireNonNullElse(file.getOriginalFilename(), "file"))
                            .getFileName().toString();
            String key = options.getPath() + "/" + safeName;

            // AWS SDK can reread file body if first sending fails, so in subsequent reads input stream
            // will have already been consumed. We should use raw bytes
            byte[] bytes = file.getBytes();

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength((long) bytes.length)
                    .metadata(Map.of("owner", String.valueOf(options.getOwner())))
                    .build();

            s3.putObject(request, RequestBody.fromBytes(bytes));
            return getSimpleUrl(key);

        } catch (IOException e) {
            throw new StorageException("Couldn't upload file " + file.getOriginalFilename());
        } catch (S3Exception e) {
            throw new StorageException("S3 upload failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    private boolean bucketExists(String bucketName) throws StorageException {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3.headBucket(headBucketRequest);
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        } catch (S3Exception e) {
            throw new StorageException("Error checking bucket existence: " + e.getMessage());
        }
    }

    private void createBucket(String bucketName) throws StorageException {
        try {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3.createBucket(createBucketRequest);

            // Optionally wait for bucket to be created
            s3.waiter().waitUntilBucketExists(
                    HeadBucketRequest.builder().bucket(bucketName).build()
            );
        } catch (BucketAlreadyExistsException e) {
            throw new StorageException("Bucket already exists: " + bucketName);
        } catch (BucketAlreadyOwnedByYouException e) {
            // Bucket already owned by you - fine, continue
            System.out.println("Bucket already owned by you: " + bucketName);
        } catch (S3Exception e) {
            throw new StorageException("Error creating bucket: " + e.getMessage());
        }
    }

    private String getSimpleUrl(String key) {
        // Construct URL manually using the endpoint from properties
        // Remove trailing slash from endpoint if present
        String baseEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return String.format("%s/%s/%s", baseEndpoint, bucketName, key);
    }

    @Override
    public void load(String filename) {
        // To be implemented
    }

    @Override
    public String getRootPath() {
        return StorageService.super.getRootPath();
    }
}