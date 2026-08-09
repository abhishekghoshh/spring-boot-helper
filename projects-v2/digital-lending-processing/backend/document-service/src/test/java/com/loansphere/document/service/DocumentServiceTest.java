package com.loansphere.document.service;

import com.loansphere.document.model.DocumentMetadata;
import com.loansphere.document.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void shouldStoreDocumentMetadata() {
        DocumentMetadata doc = new DocumentMetadata();
        doc.setUserId("user1"); doc.setFileName("id-proof.pdf");
        doc.setContentType("application/pdf"); doc.setSizeBytes(1024L);

        when(documentRepository.save(any())).thenReturn(doc);

        DocumentMetadata result = documentService.storeMetadata(doc);

        assertThat(result.getFileName()).isEqualTo("id-proof.pdf");
    }

    @Test
    void shouldGetDocumentsByUserId() {
        DocumentMetadata doc = new DocumentMetadata();
        doc.setUserId("user1"); doc.setFileName("doc.pdf");

        when(documentRepository.findByUserId("user1")).thenReturn(List.of(doc));

        List<DocumentMetadata> docs = documentService.findByUserId("user1");

        assertThat(docs).hasSize(1);
    }

    @Test
    void shouldFindDocumentById() {
        DocumentMetadata doc = new DocumentMetadata();
        doc.setId("doc1"); doc.setFileName("doc.pdf");

        when(documentRepository.findById("doc1")).thenReturn(Optional.of(doc));

        DocumentMetadata result = documentService.findById("doc1");

        assertThat(result.getFileName()).isEqualTo("doc.pdf");
    }
}
