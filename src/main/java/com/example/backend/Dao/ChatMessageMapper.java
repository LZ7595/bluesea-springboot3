package com.example.backend.Dao;

import com.example.backend.Entity.WebSocket.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    @Insert("INSERT INTO chat_message(sender_id, receiver_id, content, session_id) " +
            "VALUES(#{senderId}, #{receiverId}, #{content}, #{sessionId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMessage(Message message);

    @Update("UPDATE chat_message SET is_read = 1 WHERE session_id = #{sessionId} AND receiver_id = #{userId}")
    int markMessagesAsRead(@Param("sessionId") String sessionId, @Param("userId") Integer userId);

    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} ORDER BY send_time ASC")
    List<Message> getChatHistory(@Param("sessionId") String sessionId);
}