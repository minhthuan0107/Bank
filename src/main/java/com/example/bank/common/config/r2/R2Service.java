package com.example.bank.common.config.r2;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
    @Service
    @RequiredArgsConstructor
    public class R2Service {

        private final S3Client s3Client;

        @Value("${r2.bucket}")
        private String bucket;

        @Value("${r2.public-url}")
        private String publicUrl;

        public String upload(MultipartFile file, String orderNo) {

            String extension = getExtension(file.getOriginalFilename());

            String key =
                    "deposit-proof/" +
                            orderNo +
                            "/" +
                            UUID.randomUUID() +
                            "." +
                            extension;

            try (InputStream is = file.getInputStream()) {

                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .build();

                s3Client.putObject(
                        request,
                        RequestBody.fromInputStream(is, file.getSize())
                );

            } catch (IOException e) {
                throw new RuntimeException("R2 upload failed", e);
            }

            return publicUrl + "/" + key;
        }

        private String getExtension(String filename) {

            if (filename == null || !filename.contains(".")) {
                return "png";
            }

            String ext = filename.substring(filename.lastIndexOf('.') + 1);

            return ext.toLowerCase();
        }
    }
