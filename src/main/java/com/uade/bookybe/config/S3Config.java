package com.uade.bookybe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(name = "image.storage.strategy", havingValue = "s3")
public class S3Config {

  @Value("${aws.s3.access-key:#{null}}")
  private String accessKey;

  @Value("${aws.s3.secret-key:#{null}}")
  private String secretKey;

  @Value("${aws.s3.region:us-east-1}")
  private String region;

  @Value("${aws.s3.bucket:bucket-user-images-store}")
  private String bucketName;

  @Bean
  @ConditionalOnProperty(name = "image.storage.strategy", havingValue = "s3")
  public S3Client s3Client() {
    AwsCredentialsProvider credentialsProvider;
    
    if (accessKey != null && secretKey != null) {
      // Usar credenciales explícitas
      credentialsProvider = StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKey, secretKey));
    } else {
      // Usar el default credentials provider chain (IAM roles, etc.)
      credentialsProvider = software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.create();
    }

    return S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(credentialsProvider)
        .httpClient(ApacheHttpClient.builder().build())
        .build();
  }

  @Bean
  @ConditionalOnProperty(name = "image.storage.strategy", havingValue = "s3")
  public S3Presigner s3Presigner() {
    AwsCredentialsProvider credentialsProvider;
    
    if (accessKey != null && secretKey != null) {
      credentialsProvider = StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKey, secretKey));
    } else {
      credentialsProvider = software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.create();
    }

    return S3Presigner.builder()
        .region(Region.of(region))
        .credentialsProvider(credentialsProvider)
        .build();
  }

  @Bean
  @ConditionalOnProperty(name = "image.storage.strategy", havingValue = "s3")
  public String s3BucketName() {
    return bucketName;
  }
} 