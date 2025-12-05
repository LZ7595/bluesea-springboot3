package com.example.backend.Controller;

import com.example.backend.Dao.MessageMapper;
import com.example.backend.Dao.UserMapper;
import com.example.backend.Model.Dto.PageResult;
import com.example.backend.Model.Entity.User;
import com.example.backend.Model.WebSocket.Message;
import com.example.backend.Service.MessageService;
import com.example.backend.Utils.AssertUtils;
import com.example.backend.Utils.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 客服对话专用控制器
 */
@RestController
@RequestMapping("/service/chat")
@Slf4j
public class ServiceChatController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private WebSocketUtil webSocketUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageMapper messageMapper;

    /**
     * 接口1：用户发送消息给客服
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendToService(
            @RequestParam Integer userId,
            @RequestParam String content,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer targetServiceId) {
        try {
            log.info("用户{}发送消息给客服：{}", userId, content);
            messageService.sendToService(userId, content, type, targetServiceId);
            return ResponseEntity.ok("消息发送成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 接口2：获取在线客服列表（仅Operator角色）
     */
    @GetMapping("/online")
    public ResponseEntity<?> getOnlineServices(
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestParam("userId") Integer userId) throws Exception { // 新增：当前登录用户ID（用于查询聊天记录）
        // 1. 参数校验
        if (limit <= 0 || limit > 50) {
            limit = 5;
        }
        AssertUtils.isError(userId == 0, "登录用户ID不能为空!");
        User loginUser = userMapper.selectbyUserId(userId);
        AssertUtils.isError(loginUser == null, "登录用户不存在!");

        // 2. 从数据库查询所有客服（按数量限制）
        List<User> allServices = userMapper.selectByRoleWithLimit("Operator", limit);
        if (allServices == null || allServices.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // 3. 获取在线客服ID集合
        List<Integer> onlineServiceIds = webSocketUtil.getOnlineServiceIds();
        Set<Integer> onlineSet = new HashSet<>(onlineServiceIds);

        // 4. 组装客服信息（含最后一条消息）
        List<Map<String, Object>> serviceList = allServices.stream().map(service -> {
            Map<String, Object> info = new HashMap<>();
            Integer serviceId = service.getId();

            // 4.1 基础信息
            info.put("serviceId", serviceId);
            info.put("serviceName", service.getUsername());
            info.put("avatar", service.getAvatar());
            info.put("isOnline", onlineSet.contains(serviceId));
            info.put("status", service.isStatus());

            // 4.2 核心：查询当前用户与该客服的最后一条消息
            Message lastMessage = messageMapper.selectLastMessageBetween(
                    userId, serviceId, 1); // 查最近1条消息

            // 4.3 处理最后一条消息（文本/图片区分显示）
            if (lastMessage != null) {
                String lastMsgContent;
                if ("image".equals(lastMessage.getType())) {
                    lastMsgContent = "[图片]"; // 图片消息显示占位符
                } else {
                    lastMsgContent = lastMessage.getContent(); // 文本消息显示内容
                }
                info.put("lastMessage", lastMsgContent);
                info.put("lastMessageTime", lastMessage.getCreate_time()); // 消息时间（可选）
            } else {
                info.put("lastMessage", ""); // 无消息时为空
                info.put("lastMessageTime", null);
            }

            return info;
        }).collect(Collectors.toList());

        // 5. 排序：在线优先 → 最后消息时间最新优先 → ID升序
        serviceList.sort((s1, s2) -> {
            // 先按在线状态排序
            boolean s1Online = (boolean) s1.get("isOnline");
            boolean s2Online = (boolean) s2.get("isOnline");
            if (s1Online != s2Online) {
                return s1Online ? -1 : 1;
            }

            // 在线状态相同则按最后消息时间排序（最新的在前）
            Date time1 = (Date) s1.get("lastMessageTime");
            Date time2 = (Date) s2.get("lastMessageTime");
            if (time1 != null && time2 != null) {
                return time2.compareTo(time1); // 时间倒序（新消息在前）
            }
            if (time1 != null) return -1; // 有消息的在前
            if (time2 != null) return 1;

            // 都无消息则按ID升序
            return ((Integer) s1.get("serviceId")).compareTo((Integer) s2.get("serviceId"));
        });

        return ResponseEntity.ok(serviceList);
    }

    /**
     * 接口3：获取用户与客服的聊天记录
     */
    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(
            @RequestParam Integer userId,
            @RequestParam Integer serviceId) {
        try {
            List<Message> history = messageService.findMessageBySendUserAndReceiveUser(userId, serviceId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 分页获取聊天记录
     *
     * @param sendUserId    发送方ID
     * @param receiveUserId 接收方ID
     * @param currentPage   当前页码（默认1）
     * @param pageSize      每页条数（默认20）
     */
    @GetMapping("/history/page")
    public PageResult<Message> getMessagePage(
            @RequestParam int sendUserId,
            @RequestParam int receiveUserId,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "20") int pageSize) throws Exception {
        return messageService.findMessageByPage(sendUserId, receiveUserId, currentPage, pageSize);
    }

    @GetMapping("/serviceDetail")
    public ResponseEntity<?> getServiceDetail(@RequestParam Integer serviceId) {
        try {
            // 1. 参数校验
            AssertUtils.isError(serviceId == null || serviceId <= 0, "客服ID不能为空或无效");

            // 2. 查询客服信息（仅查询Operator角色）
            User service = userMapper.selectByRoleAndId("Operator", serviceId);
            AssertUtils.isError(service == null, "客服不存在或不是有效的客服账号");

            // 3. 判断客服是否在线
            List<Integer> onlineServiceIds = webSocketUtil.getOnlineServiceIds();
            boolean isOnline = onlineServiceIds.contains(serviceId);

            // 4. 组装与列表接口一致的字段结构
            Map<String, Object> serviceDetail = new HashMap<>();
            serviceDetail.put("serviceId", service.getId());         // 客服ID
            serviceDetail.put("serviceName", service.getUsername()); // 客服名称（与列表一致）
            serviceDetail.put("avatar", service.getAvatar());        // 头像路径
            serviceDetail.put("isOnline", isOnline);                 // 是否在线
            serviceDetail.put("status", service.isStatus());         // 账号状态（是否冻结）

            // 5. 返回结果（与列表接口单个客服数据结构完全一致）
            return ResponseEntity.ok(serviceDetail);
        } catch (Exception e) {
            log.error("获取客服详情失败：", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}