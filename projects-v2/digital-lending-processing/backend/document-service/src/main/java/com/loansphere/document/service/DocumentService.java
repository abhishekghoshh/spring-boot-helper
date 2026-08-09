package com.loansphere.document.service;

import com.loansphere.common.exception.BadRequestException;
import com.loansphere.common.exception.ResourceNotFoundException;
import com.loansphere.document.model.DocumentMetadata;
import com.loansphere.document.repository.DocumentRepository;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final MongoDatabaseFactory mongoDatabaseFactory;

    public DocumentService(DocumentRepository documentRepository, MongoDatabaseFactory mongoDatabaseFactory) {
        this.documentRepository = documentRepository;
        this.mongoDatabaseFactory = mongoDatabaseFactory;
    }

    public DocumentMetadata upload(String userId, MultipartFile file, String documentType) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum limit of 10 MB");
        }

        GridFSBucket gridFsBucket = GridFSBuckets.create(mongoDatabaseFactory.getMongoDatabase());

        String fileId;
        try (InputStream inputStream = file.getInputStream()) {
            ObjectId objectId = gridFsBucket.uploadFromStream(file.getOriginalFilename(), inputStream);
            fileId = objectId.toHexString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }

        DocumentMetadata doc = new DocumentMetadata();
        doc.setUserId(userId);
        doc.setFileName(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setFileId(fileId);
        doc.setDocumentType(documentType);
        doc.setVersion2(1);
        doc.setStatus("ACTIVE");

        return documentRepository.save(doc);
    }

    public DocumentMetadata getDocument(String id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
    }

    public void download(String id, OutputStream outputStream) {
        DocumentMetadata doc = getDocument(id);
        GridFSBucket gridFsBucket = GridFSBuckets.create(mongoDatabaseFactory.getMongoDatabase());
        gridFsBucket.downloadToStream(new ObjectId(doc.getFileId()), outputStream);
    }

    public List<DocumentMetadata> getUserDocuments(String userId) {
        return documentRepository.findByUserId(userId);
    }

    public void delete(String id) {
        DocumentMetadata doc = getDocument(id);
        GridFSBucket gridFsBucket = GridFSBuckets.create(mongoDatabaseFactory.getMongoDatabase());
        gridFsBucket.delete(new ObjectId(doc.getFileId()));
        documentRepository.delete(doc);
    }
}
