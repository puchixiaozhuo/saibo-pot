package com.xiaozhuo.test;

public class CheckEnvVars {
    public static void main(String[] args) {
        System.out.println("=== Checking OSS Environment Variables ===\n");

        String endpoint = System.getenv("OSS_ENDPOINT");
        String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        String bucketName = System.getenv("OSS_BUCKET_NAME");

        System.out.println("OSS_ENDPOINT: " + (endpoint != null ? "✓ " + endpoint : "✗ NOT SET"));
        System.out.println("OSS_ACCESS_KEY_ID: " + (accessKeyId != null ? "✓ Set" : "✗ NOT SET"));
        System.out.println("OSS_ACCESS_KEY_SECRET: " + (accessKeySecret != null ? "✓ Set" : "✗ NOT SET"));
        System.out.println("OSS_BUCKET_NAME: " + (bucketName != null ? "✓ " + bucketName : "✗ NOT SET"));

        boolean allSet = endpoint != null && accessKeyId != null &&
                        accessKeySecret != null && bucketName != null;

        System.out.println("\n" + (allSet ? "✅ All variables configured!" : "❌ Some variables missing!"));

        if (!allSet) {
            System.out.println("\nPlease configure environment variables in:");
            System.out.println("- IntelliJ IDEA: Run Configuration -> Environment variables");
            System.out.println("- Or create Tomcat/bin/setenv.bat");
        }
    }
}