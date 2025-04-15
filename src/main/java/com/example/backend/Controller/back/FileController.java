package com.example.backend.Controller.back;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class FileController {

    @Value("${image.storage.directory}")
    private String filePath;

    @PostMapping("/back/product/upload")
    public String uploadProductFiles(@RequestParam("files") MultipartFile[] files) {
        System.out.println("Received files: " + files);
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String Path = filePath + "goods/" + fileName;
            File dest = new File(Path);
            String fileUrl = "/goods/" + fileName;

            try {
                file.transferTo(dest);
                imageUrls.add(fileUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return String.join(",", imageUrls);
    }

    @PostMapping("/back/user/upload")
    public String uploadUserFiles(@RequestParam("file") MultipartFile[] files) {
        System.out.println("Received files: " + files);
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String Path = filePath + "avatar/" + fileName;
            File dest = new File(Path);
            String fileUrl = "/avatar/" + fileName;

            try {
                file.transferTo(dest);
                imageUrls.add(fileUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return String.join(",", imageUrls);
    }
    @PostMapping("/review/upload")
    public String uploadCommentFiles(@RequestParam("files") MultipartFile[] files) {
        System.out.println("Received files: " + files);
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String Path = filePath + "comment/" + fileName;
            File dest = new File(Path);
            String fileUrl = "/comment/" + fileName;

            try {
                file.transferTo(dest);
                imageUrls.add(fileUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return String.join(",", imageUrls);
    }
    @PostMapping("/back/banner/upload")
    public String uploadBannerFiles(@RequestParam("file") MultipartFile[] files) {
        System.out.println("Received files: " + files);
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String Path = filePath + "banner/" + fileName;
            File dest = new File(Path);
            String fileUrl = "/banner/" + fileName;

            try {
                file.transferTo(dest);
                imageUrls.add(fileUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return String.join(",", imageUrls);
    }

    @PostMapping("/avatar/upload")
    public String uploadAvatarFiles(@RequestParam("file") MultipartFile[] files) {
        System.out.println("Received files: " + files);
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String Path = filePath + "avatar/" + fileName;
            File dest = new File(Path);
            String fileUrl = "/avatar/" + fileName;

            try {
                file.transferTo(dest);
                imageUrls.add(fileUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return String.join(",", imageUrls);
    }
}
