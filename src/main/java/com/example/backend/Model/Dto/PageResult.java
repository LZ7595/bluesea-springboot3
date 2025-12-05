package com.example.backend.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
    // 当前页数据
    private List<T> list;
    // 总记录数
    private int total;
    // 当前页码
    private int currentPage;
    // 每页显示的记录数
    private int pageSize;
    // 总页数
    private int totalPages;
}
