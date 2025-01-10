package com.example.backend.Utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;


@Component
public class Email {

    private final JavaMailSender javaMailSender;

    @Autowired
    public Email(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }


    public void sendEmail(String toEmail, String verificationCode, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromEmail());  // 从配置文件中获取发送者邮箱
        message.setTo(toEmail);
        message.setSubject("验证码");
        message.setText(String.format("%s 您的验证码是: %s", content, verificationCode));

        try {
            javaMailSender.send(message);
            System.out.println("邮件发送成功");
        } catch (MailException e) {
            // 可以根据实际情况进行日志记录或采取其他措施
            System.err.println("邮件发送失败: " + e.getMessage());
            // 抛出运行时异常，可根据需要修改为更具体的异常
            throw new RuntimeException("邮件发送失败", e);
        }
    }


    private String getFromEmail() {
        // 从配置文件中读取发送者邮箱，假设在 application.properties 中配置了 spring.mail.username
        return "2567407595@qq.com";
    }
}