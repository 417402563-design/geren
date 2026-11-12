package cn.edu.sdua.geren.repository;

import cn.edu.sdua.geren.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByUserId(Long userId);

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    void deleteByDocumentId(Long documentId);
}
