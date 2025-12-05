package com.example.backend.Model.Entity.back;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class ExpressBack {
    /**
     * 主键ID（自增）
     */
    private Integer id;

    /**
     * 快递公司编码（对应表中eng字段）
     */
    private String eng;

    /**
     * 快递公司中文名称（对应表中chi字段）
     */
    private String chi;

    /**
     * 快递公司logo图片地址
     */
    private String imageUrl;
    private Date create_time;
    private Date update_time;
}
