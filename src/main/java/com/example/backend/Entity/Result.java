package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Integer code;
    private String msg;
    private Object data;
    private ResponseCookie cookie;

    // 增删改 成功响应
    public static Result success() {
        return new Result(200, "success", null, null);
    }

    // 查询 成功响应
    public static Result success(Object data) {
        return new Result(200, "success", data, null);
    }

    // 带有 Cookie 的成功响应
    public static Result success(Object data, ResponseCookie cookie) {
        return new Result(200, "success", data, cookie);
    }

    // 失败响应
    public static Result error(String msg, Object data) {
        return new Result(500, msg, data, null);
    }

    public ResponseEntity<?> toResponseEntity() {
        if (cookie == null) {
            return ResponseEntity.status(code).body(this);
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
            return ResponseEntity.status(code).headers(headers).body(this);
        }
    }
}