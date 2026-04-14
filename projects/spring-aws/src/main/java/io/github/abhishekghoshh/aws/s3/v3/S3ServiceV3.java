package io.github.abhishekghoshh.aws.s3.v3;

import io.github.abhishekghoshh.aws.core.redis.RedisService;
import io.github.abhishekghoshh.aws.s3.v2.dto.S3ObjectInputStreamWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.Duration;

@Service
public class S3ServiceV3 {

    private static final String REDIS_KEY_PREFIX = "s3:files";

    private static final Logger logger = LoggerFactory.getLogger(S3ServiceV3.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final RedisService redisService;

    private final String bucketName;
    private final long presignUrlTimeout;

    public S3ServiceV3(@Autowired S3Client s3Client,
                       @Autowired S3Presigner s3Presigner,
                       @Autowired RedisService redisService,
                       @Value("${cloud.aws.s3.bucket.v3}") String bucketName,
                       @Value("${cloud.aws.s3.presign-url-timeout}") long presignUrlTimeout) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.redisService = redisService;
        this.bucketName = bucketName;
        this.presignUrlTimeout = presignUrlTimeout;
    }

    /**
     * Builds a sanitized filename with a timestamp prefix.
     */
    public static String buildFilename(String filename) {
        return String.format("%s_%s", System.currentTimeMillis(), sanitizeFileName(filename));
    }

    /**
     * Sanitizes the filename by removing special characters and replacing spaces.
     */
    private static String sanitizeFileName(String fileName) {
        String normalizedFileName = Normalizer.normalize(fileName, Normalizer.Form.NFKD);
        return normalizedFileName.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9.\\-_]", "");
    }

    private static void checkIfValidFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Filename contains invalid path sequences");
        }
        String sanitizedFileName = sanitizeFileName(fileName);
        if (!sanitizedFileName.equals(fileName)) {
            throw new IllegalArgumentException("Filename contains invalid characters");
        }
    }

    /**
     * Generates a presigned GET URL.
     */
    public String generateGetPresignedUrl(String filePath) {
        if (!redisService.isMemberOfSet(REDIS_KEY_PREFIX + ":uploaded-files", filePath)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignUrlTimeout))
                .getObjectRequest(request ->
                        request.bucket(bucketName)
                                .key(filePath)
                                .build()
                )
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    /**
     * Generates a presigned PUT URL with optional ACL based on AccessType.
     */
    public String generatePutPresignedUrl(String filePath) {
        checkIfValidFileName(filePath);
        this.redisService.addToSet(REDIS_KEY_PREFIX + ":uploaded-files", filePath);
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignUrlTimeout))
                .putObjectRequest(request ->
                        request.bucket(bucketName)
                                .key(filePath)
                                .build()
                )
                .build();

        return s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();
    }


    /**
     * Uploads a MultipartFile to S3 with specified access type.
     */
    public String uploadMultipartFile(MultipartFile file) throws IOException {
        String fileName = buildFilename(file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName);

            PutObjectRequest putObjectRequest = putObjectRequestBuilder.build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
        }
        return fileName;
    }

    /**
     * Downloads a file from S3 and returns an InputStream and ETag.
     */
    public S3ObjectInputStreamWrapper downloadFile(String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        ResponseInputStream<GetObjectResponse> s3ObjectResponse = s3Client.getObject(getObjectRequest);
        String eTag = s3ObjectResponse.response().eTag();
        return new S3ObjectInputStreamWrapper(s3ObjectResponse, eTag);
    }

    /**
     * Returns a ResponseEntity with StreamingResponseBody and includes ETag in headers.
     */
    public ResponseEntity<StreamingResponseBody> downloadFileResponse(String fileName) throws IOException {
        String contentType = Files.probeContentType(Paths.get(fileName));


        S3ObjectInputStreamWrapper fileWrapper = downloadFile(fileName);

        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream inputStream = fileWrapper.inputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error occurred while streaming the file", e);
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        if (fileWrapper.eTag() != null) {
            headers.add(HttpHeaders.ETAG, fileWrapper.eTag());
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(responseBody);
    }

}
