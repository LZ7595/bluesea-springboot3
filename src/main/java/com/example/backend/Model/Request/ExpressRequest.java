package com.example.backend.Model.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;


@Data
public class ExpressRequest {
    @NotBlank(message = "快递公司编码不能为空")
    private String com;  // 快递公司编码（小写）

    @NotBlank(message = "快递单号不能为空")
    @Length(min = 6, max = 32, message = "单号长度必须在6-32位之间")
    private String num;  // 快递单号

    private String phone; // 收/寄件人电话（可选）
    private String from;  // 出发地（可选）
    private String to;    // 目的地（可选）
    private String lang = "zh";  // 语言版本，默认为中文
}
