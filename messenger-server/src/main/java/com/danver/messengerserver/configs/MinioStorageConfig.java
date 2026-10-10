package com.danver.messengerserver.configs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name="s3.storage.provider", havingValue = "minio")
public class MinioStorageConfig {

    private final Environment env;

    public MinioStorageConfig(Environment env) {
        this.env = env;
    }

    @Bean
    S3Client minioS3Client() {
        // Use the root credentials as static credentials
        AwsCredentialsProvider creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        env.getProperty("s3.storage.access-key"),
                        env.getProperty("s3.storage.secret-key")
                ));

        return S3Client.builder()
                .credentialsProvider(creds)
                .region(Region.of(env.getProperty("s3.storage.region", "us-east-1")))
                .endpointOverride(URI.create(env.getProperty("s3.storage.endpoint"))) // http://localhost:9000
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // Critical for MinIO [citation:1]
                        .build())
                .build();
    }
}