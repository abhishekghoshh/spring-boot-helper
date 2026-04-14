package io.github.abhishekghoshh.aws.s3.v2;

import io.github.abhishekghoshh.aws.s3.v2.dto.AccessType;
import io.github.abhishekghoshh.aws.s3.v2.dto.MultiplePreSignedUrlRequest;
import io.github.abhishekghoshh.aws.s3.v2.dto.MultiplePreSignedUrlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.github.abhishekghoshh.aws.s3.v2.S3ServiceV2.buildFilename;

/**
 * S3ControllerV2 provides REST endpoints for generating presigned URLs for S3 operations.
 * It allows clients to obtain temporary URLs for uploading and downloading files from S3 without needing AWS credentials.
 */
@RestController
@RequestMapping("/api/v2/s3")
public class S3ControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(S3ControllerV2.class);

    private final S3ServiceV2 s3Service;

    public S3ControllerV2(S3ServiceV2 s3Service) {
        this.s3Service = s3Service;
    }

    /**
     * Generates a presigned GET URL for a file.
     * This endpoint allows clients to obtain a temporary URL that can be used to download a file directly
     * from S3 without needing to authenticate with AWS credentials. The URL will be valid for a limited time,
     * typically 60 minutes, and can be used by anyone who has the link to access the file.
     */
    @GetMapping("/pre-signed-url/{filename}")
    public ResponseEntity<String> getUrl(@PathVariable String filename) {
        String url = s3Service.generateGetPresignedUrl(filename);
        return ResponseEntity.ok(url);
    }

    /**
     * Generates a presigned PUT URL with specified access type.
     */
    @PostMapping("/pre-signed-url")
    public ResponseEntity<Map<String, Object>> generateUrl(
            @RequestParam(name = "filename", required = false, defaultValue = "") String filename,
            @RequestParam(name = "accessType", required = false, defaultValue = "PRIVATE") AccessType accessType) {
        filename = buildFilename(filename);
        String url = s3Service.generatePutPresignedUrl(filename, accessType);
        return ResponseEntity.ok(Map.of("url", url, "file", filename));
    }

    /**
     * Generates multiple presigned PUT URLs with specified access type.
     */
    @PostMapping("/pre-signed-urls")
    public ResponseEntity<Map<String, List<MultiplePreSignedUrlResponse>>> generateUrls(
            @RequestBody List<MultiplePreSignedUrlRequest> request,
            @RequestParam(name = "accessType", required = false, defaultValue = "PRIVATE") AccessType accessType) {
        List<MultiplePreSignedUrlResponse> urls = s3Service.generateMultiplePreSignedUrls(request, accessType);
        return ResponseEntity.ok(Map.of("urls", urls));
    }

    /**
     * Uploads a file with specified access type.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "accessType", required = false, defaultValue = "PRIVATE") AccessType accessType) throws IOException {
        String fileName = s3Service.uploadMultipartFile(file, accessType);
        return ResponseEntity.ok("File name: " + fileName);
    }

    /**
     * Downloads a file.
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable("fileName") String fileName) throws Exception {
        return s3Service.downloadFileResponse(fileName);
    }

}
