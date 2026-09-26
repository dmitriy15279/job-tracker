package com.example.jobtracker.service.avatar;

import com.example.jobtracker.config.AvatarProperties;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
@Component
public class AvatarStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public AvatarStorage(S3Client s3Client, S3Presigner s3Presigner, AvatarProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = properties.s3().bucket();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createBucketIfMissing() {
        try {
            s3Client.headBucket(request -> request.bucket(bucket));
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(request -> request.bucket(bucket));
            log.info("Created bucket {}", bucket);
        } catch (SdkException e) {
            log.warn("Could not check bucket {}: {}", bucket, e.getMessage());
        }
    }

    public void put(String key, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(content));
    }

    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }

    public String createDownloadUrl(String key, Duration ttl) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getRequest)
                .signatureDuration(ttl)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
