package com.danver.messengerserver.configs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name="s3.storage.provider", havingValue = "filebase")
public class FilebaseStorageConfig {

    private final Environment env;

    public FilebaseStorageConfig(Environment env) {
        this.env = env;
    }

    @Bean
    S3Client getAmazonS3Client() {
        // Alternative way of getting credentials (commented - using BasicAwsCredentials)
        /*
        AwsCredentials credentials = AwsBasicCredentials.create(
            this.env.getProperty("yandex.cloud.object-storage.account.access.key.id"),
            this.env.getProperty("yandex.cloud.object-storage.account.secret.key")
        );
        */

        // Get credentials from profile (~/.aws/credentials)
        AwsCredentialsProvider credentialsProvider;
        try {
            // ProfileCredentialsProvider get credentials from profile (~/.aws/credentials)
            credentialsProvider = ProfileCredentialsProvider.create();
            // Test the credentials provider by calling resolveCredentials()
            credentialsProvider.resolveCredentials();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        // Build S3 client with path-style access for Yandex Cloud / S3-compatible storage
        return S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(env.getProperty("s3.storage.signingRegion")))
                .endpointOverride(URI.create(env.getProperty("s3.storage.endpoint")))
                // ── S3-specific: disable chunked encoding ──────────────────────
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)    // Disable aws-chunked transfer encoding
                        .build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
/*        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(this.env.getProperty("s3.storage.signingRegion")))
                .endpointOverride(URI.create(this.env.getProperty("s3.storage.endpoint")))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();*/
    }
}