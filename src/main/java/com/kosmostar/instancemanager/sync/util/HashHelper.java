package com.kosmostar.instancemanager.sync.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class HashHelper {

    public static String hashString(String input) {
        String normalized = input.replace("\r\n", "\n");
        return sha256(normalized.getBytes(StandardCharsets.UTF_8));
    }

    public static String hashFile(Path file) {
        if (!Files.exists(file) || Files.isDirectory(file)) return "";

        try {
            // Normalize text files
            String fileName = file.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".json") || fileName.endsWith(".txt") || fileName.endsWith(".properties") || fileName.endsWith(".toml")) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                return hashString(content);
            }

            // Raw byte hash for binary files (e.g. .jar, .png, .dat)
            try (InputStream is = Files.newInputStream(file)) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
                return bytesToHex(digest.digest());
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(bytes));
        } catch (Exception e) {
            return "";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}