package com.example.backend.Dao;

import com.example.backend.Entity.WebSocket.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageDao {

    // 根据接收用户和限制长度查询消息
    @Select("SELECT * FROM message WHERE receive_user = #{receiveUser} ORDER BY create_time DESC LIMIT #{limit}")
    List<Message> selectByReceiveUserLimitLength(@Param("receiveUser") int receiveUser, @Param("limit") Integer limit);

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
}