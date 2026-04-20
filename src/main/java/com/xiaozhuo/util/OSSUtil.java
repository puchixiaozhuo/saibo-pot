package com.xiaozhuo.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

public class OSSUtil {
    private static String endpoint;
    private static String accessKeyId;
    private static String accessKeySecret;
    private static String bucketName;
    private static String domain;

    static {
        try {
            Properties prop = new Properties();
            InputStream is = OSSUtil.class.getClassLoader().getResourceAsStream("db.properties");
            if (is != null) {
                prop.load(is);
                endpoint = prop.getProperty("oss.endpoint", "");
                accessKeyId = prop.getProperty("oss.accessKeyId", "");
                accessKeySecret = prop.getProperty("oss.accessKeySecret", "");
                bucketName = prop.getProperty("oss.bucketName", "");
                domain = prop.getProperty("oss.domain", "");

                if (!endpoint.isEmpty()) {
                    System.out.println("OSS配置加载成功！");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String uploadFile(InputStream inputStream, String fileName, long fileSize) {
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            String objectName = generateObjectName(fileName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileSize);

            ossClient.putObject(bucketName, objectName, inputStream, metadata);

            if (domain != null && !domain.isEmpty()) {
                return domain + "/" + objectName;
            } else {
                return "https://" + bucketName + "." + endpoint + "/" + objectName;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("文件上传到OSS失败：" + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public static String uploadFile(byte[] fileBytes, String fileName) {
        return uploadFile(new ByteArrayInputStream(fileBytes), fileName, fileBytes.length);
    }

    public static boolean deleteFile(String fileUrl) {
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            String objectName = extractObjectName(fileUrl);
            ossClient.deleteObject(bucketName, objectName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private static String generateObjectName(String fileName) {
        String extension = "";
        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf("."));
        }
        return "videos/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private static String extractObjectName(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "";
        }

        String prefix;
        if (domain != null && !domain.isEmpty()) {
            prefix = domain + "/";
        } else {
            prefix = "https://" + bucketName + "." + endpoint + "/";
        }

        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }
        return fileUrl;
    }
}
