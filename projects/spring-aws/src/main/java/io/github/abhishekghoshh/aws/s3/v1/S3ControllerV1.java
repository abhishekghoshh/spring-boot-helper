package io.github.abhishekghoshh.aws.s3.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.List;

/**
 * This bucket is a private bucket that allows file uploads and downloads using the S3Client only
 * and does not support presigned URLs. It is intended for use cases where you want to manage file uploads
 * and downloads directly through your backend service without exposing presigned URLs to clients.
 * S3ControllerV1 provides REST endpoints for file upload, download, and listing files in an S3 bucket.
 * This controller uses the S3ServiceV1 to interact with AWS S3.
 */
@RestController
@RequestMapping("/api/v1/s3")
public class S3ControllerV1 {


    private final S3ServiceV1 s3ServiceV1;

    public S3ControllerV1(S3ServiceV1 s3ServiceV1) {
        this.s3ServiceV1 = s3ServiceV1;
    }


    /**
     * Handles direct file upload to S3 using multipart/form-data.
     * This endpoint allows clients to upload files directly to S3 without needing to generate a presigned URL first.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        s3ServiceV1.uploadFile(file);
        return ResponseEntity.ok("File uploaded successfully");
    }

    /**
     * Handles file download from S3 by returning the file data as a byte array.
     * This endpoint allows clients to download files directly from S3 by specifying the filename.
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable("fileName") String fileName) {
        byte[] fileData = s3ServiceV1.downloadFile(fileName);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body(fileData);
    }

    @GetMapping("/list")
    public ResponseEntity<List<S3Object>> listFiles() {
        return ResponseEntity.ok(s3ServiceV1.listFiles());
    }
}
