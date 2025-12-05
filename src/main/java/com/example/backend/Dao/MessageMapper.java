package com.example.backend.Dao;

import com.example.backend.Model.WebSocket.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper {

    // 根据发送用户和接收用户以及限制长度查询消息
    @Select("SELECT * FROM message WHERE send_user = #{sendUser} AND receive_user = #{receiveUser} ORDER BY create_time DESC LIMIT #{limit}")
    List<Message> selectBySendUserAndReceiveUserLimitLength(@Param("sendUser") int sendUser, @Param("receiveUser") int receiveUser, @Param("limit") Integer limit);

    // 根据接收用户查询消息
    @Select("SELECT * FROM message WHERE receive_user = #{receiveUser} ORDER BY create_time DESC")
    List<Message> selectByReceiveUser(@Param("receiveUser") int receiveUser);

    // 根据发送用户查询消息
    @Select("SELECT * FROM message WHERE send_user = #{sendUser} ORDER BY create_time DESC")
    List<Message> selectBySendUser(@Param("sendUser") int sendUser);

    // 插入消息
    @Insert("INSERT INTO message (handle, send_user, receive_user, content, is_read, create_time, type) " +
            "VALUES (#{handle}, #{send_user}, #{receive_user}, #{content}, #{is_read}, #{create_time}, #{type})")
    void insert(Message message);

    // 更新消息
    @Update("UPDATE message SET is_read = '1' WHERE handle = #{handle}")
    void update(@Param("handle") String handle);

    /**
     * 分页查询两人之间的聊天记录（双向：A→B 和 B→A）
     *
     * @param sendUserId    发送方ID
     * @param receiveUserId 接收方ID
     * @param offset        偏移量（从第几条开始查）
     * @param pageSize      每页条数
     * @return 分页消息列表
     */
    @Select("SELECT * FROM message WHERE (send_user = #{sendUserId} AND receive_user = #{receiveUserId}) OR (send_user = #{receiveUserId} AND receive_user = #{sendUserId}) ORDER BY create_time DESC LIMIT #{offset}, #{pageSize}")
    List<Message> selectBySendReceivePage(
            @Param("sendUserId") int sendUserId,
            @Param("receiveUserId") int receiveUserId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    /**
     * 查询两人之间的聊天总记录数（双向：A→B 和 B→A）
     *
     * @param sendUserId    发送方ID
     * @param receiveUserId 接收方ID
     * @return 总记录数
     */
    @Select("SELECT COUNT(*) FROM message WHERE (send_user = #{sendUserId} AND receive_user = #{receiveUserId}) OR (send_user = #{receiveUserId} AND receive_user = #{sendUserId})")
    int countBySendReceive(
            @Param("sendUserId") int sendUserId,
            @Param("receiveUserId") int receiveUserId);

    @Select("SELECT * FROM message " +
            "WHERE (send_user = #{userId1} AND receive_user = #{userId2}) " +
            "   OR (send_user = #{userId2} AND receive_user = #{userId1}) " +
            "ORDER BY create_time DESC " + // 按时间倒序，最新的消息排在最前
            "LIMIT #{limit}") // 只取1条
    Message selectLastMessageBetween(
            @Param("userId1") Integer userId1,
            @Param("userId2") Integer userId2,
            @Param("limit") Integer limit);
}