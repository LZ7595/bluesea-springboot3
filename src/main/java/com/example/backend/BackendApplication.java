package com.example.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@MapperScan({"com.example.backend.Dao"})
@EnableScheduling
@MapperScan("com.example.backend.Entity.ccc")
public class BackendApplication {

    public static void main(String[] args) {
         SpringApplication.run(BackendApplication.class, args);
    }
}