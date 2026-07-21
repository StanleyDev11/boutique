package com.example.boutique.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private static final String UPLOADS_DIR = "boutique-uploads";

    public FileStorageService() {
        String userHome = System.getProperty("user.home");
        this.fileStorageLocation = Paths.get(userHome, UPLOADS_DIR).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        return store(file, null);
    }

    public String storeProductImage(MultipartFile file) {
        return store(file, "products");
    }

    private String store(MultipartFile file, String subfolder) {
        // Normalize file name
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new RuntimeException("File name is null");
        }
        String fileName = UUID.randomUUID().toString() + "-" + originalFileName;

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path targetDir = this.fileStorageLocation;
            if (subfolder != null && !subfolder.isBlank()) {
                targetDir = this.fileStorageLocation.resolve(subfolder);
                Files.createDirectories(targetDir);
            }

            // Copy file to the target location
            Path targetLocation = targetDir.resolve(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            String storedPath = "/uploads/";
            if (subfolder != null && !subfolder.isBlank()) {
                storedPath += subfolder + "/";
            }
            storedPath += fileName;

            return storedPath;

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank() || !filePath.startsWith("/uploads/")) {
            return;
        }

        try {
            String fileName = filePath.substring("/uploads/".length());
            Path targetLocation = this.fileStorageLocation.resolve(fileName).normalize();

            if (Files.exists(targetLocation)) {
                Files.delete(targetLocation);
                System.out.println("Deleted old file: " + targetLocation);
            }
        } catch (IOException ex) {
            System.err.println("Could not delete file: " + filePath + ". " + ex.getMessage());
            // It's better to log this error instead of throwing a runtime exception,
            // as failing to delete the old file should not prevent the new one from being saved.
        }
    }
}
