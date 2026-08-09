package com.loansphere.document.repository;

import com.loansphere.document.model.DocumentMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DocumentRepository extends MongoRepository<DocumentMetadata, String> {
    List<DocumentMetadata> findByUserId(String userId);
    List<DocumentMetadata> findByUserIdAndDocumentType(String userId, String documentType);
    List<DocumentMetadata> findByStatus(String status);
}
