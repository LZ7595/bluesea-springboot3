// com.example.backend.Utils.Email.java
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

    /**
     * 发送验证码邮件
     * @param toEmail 收件邮箱
     * @param verificationCode 验证码
     * @param scene 场景描述
     */
    public void sendEmail(String toEmail, String verificationCode, String scene) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(getFromEmail());
        message.setTo(toEmail);
        message.setSubject(scene + "验证码");
        message.setText(buildEmailContent(toEmail, verificationCode, scene));

        try {
            javaMailSender.send(message);
            System.out.println(scene + "邮件发送成功至: " + toEmail);
        } catch (MailException e) {
            System.err.println("邮件发送失败: " + e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String email, String code, String scene) {
        return String.format(
                "%s验证码\n\n" +
                        "尊敬的%s用户：\n" +
                        "您的%s验证码为：%s\n\n" +
                        "验证码有效期5分钟，请及时使用。\n" +
                        "如非本人操作，请忽略此邮件。\n\n" +
                        "此为系统邮件，请勿回复",
                scene, email, scene, code
        );
    }

    private String getFromEmail() {
        return fromEmail;
    }
}