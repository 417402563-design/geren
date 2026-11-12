package cn.edu.sdua.geren.repository;

import cn.edu.sdua.geren.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByIdAsc(Long conversationId);

    void deleteByConversationId(Long conversationId);
}
