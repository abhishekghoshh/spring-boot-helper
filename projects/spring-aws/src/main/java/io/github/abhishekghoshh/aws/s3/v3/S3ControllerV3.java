package io.github.abhishekghoshh.aws.s3.v3;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v3/s3")
public class S3ControllerV3 {

    private final S3ServiceV3 s3Service;

    public S3ControllerV3(S3ServiceV3 s3Service) {
        this.s3Service = s3Service;
    }

    /**
     * Generates a presigned PUT URL with specified access type.
     */
    @PostMapping("/pre-signed-url/{filename}")
    public ResponseEntity<Map<String, Object>> generateUrl(@PathVariable String filename) {
        String url = s3Service.generatePutPresignedUrl(filename);
        return ResponseEntity.ok(Map.of("url", url, "file", filename));
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
     * Uploads a file with specified access type.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file) throws IOException {
        String fileName = s3Service.uploadMultipartFile(file);
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
