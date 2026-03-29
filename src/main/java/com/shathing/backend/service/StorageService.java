package com.shathing.backend.service;

import com.shathing.backend.dto.request.CreatePresignedUploadUrlRequest;
import com.shathing.backend.dto.response.PresignedUploadUrlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${r2.endpoint:}")
    private String r2Endpoint;

    @Value("${r2.access-key-id:}")
    private String accessKeyId;

    @Value("${r2.secret-access-key:}")
    private String secretAccessKey;

    @Value("${r2.bucket-name:}")
    private String bucketName;

    @Value("${r2.public-base-url:}")
    private String publicBaseUrl;

    @Value("${r2.presigned-url-expiration-minutes:10}")
    private long presignedUrlExpirationMinutes;

    @Transactional(readOnly = true)
    public PresignedUploadUrlResponse createPresignedUploadUrl(
            Long memberId,
            CreatePresignedUploadUrlRequest request
    ) {
        validateR2Config();

        String key = buildObjectKey(memberId, request.getFileName());
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(request.getContentType())
                .build();

        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(r2Endpoint))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .build()) {

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                    PutObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                            .putObjectRequest(putObjectRequest)
                            .build()
            );

            return new PresignedUploadUrlResponse(
                    key,
                    presignedRequest.url().toString(),
                    buildPublicUrl(key)
            );
        }
    }

    private void validateR2Config() {
        if (r2Endpoint.isBlank() || accessKeyId.isBlank() || secretAccessKey.isBlank() || bucketName.isBlank()) {
            throw new IllegalStateException("R2 configuration is incomplete.");
        }
    }

    private String buildObjectKey(Long memberId, String fileName) {
        String extension = extractExtension(fileName);
        String uuid = UUID.randomUUID().toString();
        return "share/" + memberId + "/" + uuid + extension;
    }

    private String extractExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(extensionIndex);
    }

    private String buildPublicUrl(String key) {
        if (publicBaseUrl.isBlank()) {
            return null;
        }
        return publicBaseUrl.endsWith("/")
                ? publicBaseUrl + key
                : publicBaseUrl + "/" + key;
    }
}
