package com.example.backend.Controller;

import com.example.backend.Entity.ExpressRequest;
import com.example.backend.Service.ExpressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/express")
@Validated
public class ExpressController {

    @Autowired
    private ExpressService expressService;

    /**
     * 快递查询接口
     */
    @PostMapping("/query")
    public ResponseEntity<?> query(@Valid @RequestBody ExpressRequest request) {
        return expressService.queryExpress(request);
    }
}