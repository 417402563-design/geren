package cn.edu.sdua.geren.repository;

import cn.edu.sdua.geren.entity.KbDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<KbDocument, Long> {

    List<KbDocument> findByUserIdOrderByCreatedAtDesc(Long userId);
}
