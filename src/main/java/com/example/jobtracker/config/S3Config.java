package com.example.jobtracker.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(AvatarProperties properties) {
        AvatarProperties.S3 s3 = properties.s3();
        return S3Client.builder()
                .endpointOverride(URI.create(s3.endpoint()))
                .region(Region.of(s3.region()))
                .credentialsProvider(credentials(s3))
                .forcePathStyle(true)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(AvatarProperties properties) {
        AvatarProperties.S3 s3 = properties.s3();
        return S3Presigner.builder()
                .endpointOverride(URI.create(s3.publicEndpoint()))
                .region(Region.of(s3.region()))
                .credentialsProvider(credentials(s3))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private static StaticCredentialsProvider credentials(AvatarProperties.S3 s3) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(s3.accessKey(), s3.secretKey()));
    }
}
