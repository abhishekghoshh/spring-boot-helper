package io.github.abhishekghoshh.aws.s3.v2.dto;

import java.io.InputStream;

public record S3ObjectInputStreamWrapper(InputStream inputStream, String eTag) {
}