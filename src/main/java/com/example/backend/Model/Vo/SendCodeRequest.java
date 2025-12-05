// com.example.backend.Model.Vo.SendCodeRequest.java
package com.example.backend.Model.Vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendCodeRequest {

    @NotNull(message = "目标不能为空")
    private String target;      // 目标（邮箱/手机号）

    @NotNull(message = "验证方式不能为空")
    private Integer method;     // 验证方式：0-邮箱, 1-手机, 2-旧密码

    @NotNull(message = "操作类型不能为空")
    private Integer operation;  // 操作类型：0-注册, 1-登录, 2-换绑, 3-绑定, 4-修改密码

    private Integer checkType;  // 检查类型：0-验证原绑定, 1-验证新绑定

    @Override
    public String toString() {
        return "SendCodeRequest{" +
                "target='" + target + '\'' +
                ", method=" + method +
                ", operation=" + operation +
                ", checkType=" + checkType +
                '}';
    }
}