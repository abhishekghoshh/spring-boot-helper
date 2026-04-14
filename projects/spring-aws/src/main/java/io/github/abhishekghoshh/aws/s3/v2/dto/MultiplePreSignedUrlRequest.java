package io.github.abhishekghoshh.aws.s3.v2.dto;

public class MultiplePreSignedUrlRequest {
    private String originalFileName;

    public MultiplePreSignedUrlRequest() {
    }

    public MultiplePreSignedUrlRequest(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
}
