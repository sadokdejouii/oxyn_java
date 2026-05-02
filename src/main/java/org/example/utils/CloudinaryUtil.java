package org.example.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CloudinaryUtil {

    private static Cloudinary cloudinary;

    static {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dlhz8gtag",
                "api_key", "243455364416516",
                "api_secret", "vSpzMEFj_IbiJ6m6rqtEwUphS0o"
        ));
    }

    /**
     * Uploads a file to Cloudinary and returns the secure URL.
     * Handles images, videos, and audio files with appropriate resource types.
     *
     * @param filePath the local path to the file
     * @return the secure URL string, or null if upload fails
     */
    public static String uploadMedia(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("Cloudinary upload failed: File does not exist at " + filePath);
                return null;
            }

            // Determine resource type based on file extension
            String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
            String resourceType = "auto"; // Default: let Cloudinary detect
            
            if (extension.matches("mp4|avi|mov|mkv|flv|wmv")) {
                resourceType = "video";
            } else if (extension.matches("mp3|wav|flac|aac|ogg|m4a")) {
                resourceType = "video"; // Cloudinary handles audio as video resource type
            } else if (extension.matches("png|jpg|jpeg|gif|webp|bmp|svg")) {
                resourceType = "image";
            }

            System.out.println("Uploading " + resourceType + " file: " + filePath);

            // Upload with appropriate resource type
            Map<String, Object> params = ObjectUtils.asMap(
                "resource_type", resourceType
            );
            
            Map uploadResult = cloudinary.uploader().upload(file, params);
            
            // Return the secure URL
            String secureUrl = (String) uploadResult.get("secure_url");
            System.out.println("Upload successful: " + secureUrl);
            return secureUrl;

        } catch (IOException e) {
            System.err.println("Error uploading media to Cloudinary: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets the resource type for a given file path.
     * @param filePath the file path
     * @return "image", "video", or "raw"
     */
    public static String getResourceType(String filePath) {
        if (filePath == null || filePath.isEmpty()) return "image";
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        if (extension.matches("mp4|avi|mov|mkv|flv|wmv")) return "video";
        if (extension.matches("mp3|wav|flac|aac|ogg|m4a")) return "audio";
        return "image";
    }
}
