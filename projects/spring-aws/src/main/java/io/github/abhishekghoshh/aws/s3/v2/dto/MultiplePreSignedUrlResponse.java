package io.github.abhishekghoshh.aws.s3.v2.dto;

public class MultiplePreSignedUrlResponse {
    private String url;
    private String file;
    private String originalFileName;

    public MultiplePreSignedUrlResponse() {
    }

    public MultiplePreSignedUrlResponse(String url, String file, String originalFileName) {
        this.url = url;
        this.file = file;
        this.originalFileName = originalFileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
}
