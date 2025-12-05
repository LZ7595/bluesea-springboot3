package com.example.backend.Model.Vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OperationExecuteRequest {

    @NotNull(message = "验证令牌不能为空")
    private String verifyToken;     // 验证令牌

    private String newPassword;     // 新密码（修改密码时）
    private String newTarget;       // 新绑定目标（换绑时）
    private Integer newMethod;      // 新验证方式（换绑时）
    private String code;            // 验证码(换绑时)
    @Override
    public String toString() {
        return "OperationExecuteRequest{" +
                "verifyToken='" + verifyToken + '\'' +
                ", newPassword='" + newPassword + '\'' +
                ", newTarget='" + newTarget + '\'' +
                ", newMethod=" + newMethod +
                '}';
    }
}