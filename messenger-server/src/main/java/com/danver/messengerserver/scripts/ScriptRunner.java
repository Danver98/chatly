package com.danver.messengerserver.scripts;

import com.danver.messengerserver.services.permission.PermissionType;
import com.danver.messengerserver.services.permission.ResourceType;
import com.danver.messengerserver.utils.Constants;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.net.URI;

@Component
public class ScriptRunner {
    @Autowired
    S3Client s3;
    @Autowired
    Environment env;
    @Autowired
    RedisTemplate<String, ?> redis;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void createBucket() {
        String bucketName = "filebase-bucket-name-2";
        try {
            // Create S3 client with custom endpoint (Filebase)
            S3Client s3Client = S3Client.builder()
                    .credentialsProvider(ProfileCredentialsProvider.create())
                    .region(Region.US_EAST_1)
                    .endpointOverride(URI.create("https://s3.filebase.com"))
                    .build();

            // Check if bucket exists
            if (!bucketExists(s3Client, bucketName)) {
                // Create bucket
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();
                s3Client.createBucket(createBucketRequest);

                // Wait for bucket to exist
                try (S3Waiter waiter = s3Client.waiter()) {
                    waiter.waitUntilBucketExists(
                            HeadBucketRequest.builder().bucket(bucketName).build()
                    );
                }

                // Get bucket location
                String bucketLocation = getBucketLocation(s3Client, bucketName);
                System.out.println("Bucket location: " + bucketLocation);
            }
        } catch (S3Exception e) {
            System.err.println("An Amazon S3 error occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (SdkClientException e) {
            System.err.println("An SDK client error occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean bucketExists(S3Client s3Client, String bucketName) {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.headBucket(headBucketRequest);
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        } catch (S3Exception e) {
            System.err.println("Error checking bucket existence: " + e.getMessage());
            return false;
        }
    }

    private String getBucketLocation(S3Client s3Client, String bucketName) {
        try {
            GetBucketLocationRequest locationRequest = GetBucketLocationRequest.builder()
                    .bucket(bucketName)
                    .build();
            GetBucketLocationResponse locationResponse = s3Client.getBucketLocation(locationRequest);
            return locationResponse.locationConstraintAsString();
        } catch (S3Exception e) {
            System.err.println("Error getting bucket location: " + e.getMessage());
            return "Unknown";
        }
    }

    @PostConstruct
    public void enableVersioning() {
        try {
            String bucketName = env.getProperty("s3.storage.bucket");

            if (bucketName == null || bucketName.isEmpty()) {
                System.err.println("Bucket name not configured in properties");
                return;
            }

            // 1. Enable versioning on the bucket
            PutBucketVersioningRequest putVersioningRequest = PutBucketVersioningRequest.builder()
                    .bucket(bucketName)
                    .versioningConfiguration(
                            VersioningConfiguration.builder()
                                    .status(BucketVersioningStatus.ENABLED)
                                    .build()
                    )
                    .build();

            s3.putBucketVersioning(putVersioningRequest);

            // 2. Get bucket versioning configuration information
            GetBucketVersioningRequest getVersioningRequest = GetBucketVersioningRequest.builder()
                    .bucket(bucketName)
                    .build();

            GetBucketVersioningResponse versioningResponse = s3.getBucketVersioning(getVersioningRequest);
            String status = versioningResponse.status() != null ?
                    versioningResponse.status().toString() : "Not enabled";

            System.out.println("Bucket versioning configuration status: " + status);

        } catch (S3Exception e) {
            System.out.format("An Amazon S3 error occurred. Exception: %s%n", e.getMessage());
            e.printStackTrace();
        } catch (Exception ex) {
            System.out.format("Exception: %s%n", ex.getMessage());
            ex.printStackTrace();
        }
    }

    @PostConstruct
    public void fillRedisUserPermissions() {
/*        String PERMISSION_KEY = Constants.REDIS_USERS_PERMISSIONS;
        HashOperations<String, String, String> hashOps = redis.opsForHash();
        int resourceType = ResourceType.CHAT.getValue();
        int [] userList = new int[] {
                16,
                17,
                18,
                20,
                21,
                22,
                21,
                20,
                24,
                25,
                26,
                27,
                21,
                26,
                27,
                26,
                27,
                25,
                21,
                24,
                26,
                24,
                21,
                21,
                27,
                21,
                24,
                25
        };

        int [] resourceList = new int[] {
                6,
                6,
                6,
                6,
                6,
                6,
                19,
                19,
                6,
                6,
                6,
                6,
                65,
                65,
                65,
                66,
                66,
                66,
                66,
                67,
                67,
                68,
                68,
                69,
                69,
                70,
                70,
                70
        };
        for (int i=0; i < userList.length; i++) {
            String key = userList[i] + ":" + resourceList[i] + ":" + resourceType;
            try {
                String permissionsStr = "[ " + PermissionType.Chat.DEFAULT.getValue() + "]";
                hashOps.put(PERMISSION_KEY, key, permissionsStr);
            } catch (RedisConnectionFailureException ignored) {
                int a = 1;
            }
        }
        int a = 1;*/
    }
}