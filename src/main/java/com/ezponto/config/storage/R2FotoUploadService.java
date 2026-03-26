package com.ezponto.config.storage;

import com.ezponto.application.ponto.FotoUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class R2FotoUploadService implements FotoUploadService {

    @Value("${app.storage.r2.endpoint:}")
    private String endpoint;

    @Value("${app.storage.r2.access-key:}")
    private String accessKey;

    @Value("${app.storage.r2.secret-key:}")
    private String secretKey;

    @Value("${app.storage.r2.bucket:}")
    private String bucket;

    @Override
    public String upload(String base64, Long funcionarioId) {
        if (endpoint == null || endpoint.isBlank()) {
            log.warn("R2 não configurado — foto não será salva");
            return null;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            String chave = "pontos/" + funcionarioId + "/"
                    + OffsetDateTime.now().toLocalDate() + "/"
                    + UUID.randomUUID() + ".jpg";

            S3Client s3 = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .region(Region.of("auto"))
                    .build();

            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(chave)
                            .contentType("image/jpeg")
                            .build(),
                    RequestBody.fromBytes(bytes));

            return endpoint + "/" + bucket + "/" + chave;

        } catch (Exception e) {
            log.error("Erro ao fazer upload da foto: {}", e.getMessage());
            return null;
        }
    }
}
