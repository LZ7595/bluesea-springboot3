package com.example.backend.Model.Vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IdentityVerifyRequest {

    @NotNull(message = "目标不能为空")
    private String target;      // 邮箱/手机号

    @NotNull(message = "验证码不能为空")
    private String code;        // 验证码/密码

    @NotNull(message = "验证方式不能为空")
    private Integer method;     // 0:邮箱, 1:手机, 2:密码

    @NotNull(message = "操作类型不能为空")
    private Integer operation;  // 操作类型

    private Integer checkType;  // 检查类型（换绑时使用）

    @Override
    public String toString() {
        return "IdentityVerifyRequest{" +
                "target='" + target + '\'' +
                ", code='" + code + '\'' +
                ", method=" + method +
                ", operation=" + operation +
                ", checkType=" + checkType +
                '}';
    }
}