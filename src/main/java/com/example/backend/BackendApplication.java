package com.example.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication
@MapperScan({"com.example.backend.Dao", "com.example.backend.Entity.ccc"})
@EnableScheduling
@EnableTransactionManagement
@Configuration
public class BackendApplication {

    public static void main(String[] args) {
         SpringApplication.run(BackendApplication.class, args);
    }
}