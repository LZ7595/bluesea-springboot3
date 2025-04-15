package com.example.backend.Utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class Email {

    @Value("${spring.mail.username}")
    private String fromEmail;

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
            System.err.println("邮件发送失败: " + e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }}
    private String getFromEmail() {return fromEmail;}
}