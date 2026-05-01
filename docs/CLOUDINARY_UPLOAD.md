# Cloudinary Upload System

## Overview

The Cloudinary Upload System is a utility class (`CloudinaryUtil`) that handles uploading media files (images, videos, and audio) to Cloudinary, a cloud-based media management platform. This system is integrated into the forum application to allow users to attach media files to their posts.

## Architecture

### Main Class: `CloudinaryUtil`

**Location:** `src/main/java/org/example/utils/CloudinaryUtil.java`

The utility class uses the Cloudinary SDK to upload files and manages resource type detection for different file formats.

## Configuration

### Cloudinary Credentials

The system is configured with the following Cloudinary credentials (stored in the static initializer):

- **Cloud Name:** `dlhz8gtag`
- **API Key:** `243455364416516`
- **API Secret:** `vSpzMEFj_IbiJ6m6rqtEwUphS0o`

> **Security Note:** In a production environment, these credentials should be stored in environment variables or a secure configuration file, not hardcoded in the source code.

## Features

### 1. Resource Type Detection

The system automatically detects the resource type based on file extension:

| File Extensions | Resource Type |
|-----------------|---------------|
| png, jpg, jpeg, gif, webp, bmp, svg | `image` |
| mp4, avi, mov, mkv, flv, wmv | `video` |
| mp3, wav, flac, aac, ogg, m4a | `video` (Cloudinary handles audio as video resource type) |
| Other formats | `auto` (Cloudinary auto-detection) |

### 2. File Upload

The main upload method handles:
- File existence validation
- Resource type detection
- Upload to Cloudinary with appropriate parameters
- Secure URL extraction
- Error handling with detailed logging

## API Methods

### `uploadMedia(String filePath)`

**Purpose:** Uploads a file to Cloudinary and returns the secure URL.

**Parameters:**
- `filePath` (String): The local path to the file to upload

**Returns:**
- `String`: The secure URL of the uploaded file, or `null` if upload fails

**Example Usage:**
```java
String filePath = "C:/Users/John/Desktop/image.jpg";
String uploadedUrl = CloudinaryUtil.uploadMedia(filePath);
if (uploadedUrl != null) {
    System.out.println("File uploaded successfully: " + uploadedUrl);
} else {
    System.out.println("Upload failed");
}
```

**Error Handling:**
- Returns `null` if file path is null or empty
- Returns `null` if file does not exist
- Logs error messages to console on IOException

### `getResourceType(String filePath)`

**Purpose:** Determines the resource type for a given file path.

**Parameters:**
- `filePath` (String): The file path to analyze

**Returns:**
- `String`: "image", "video", or "audio"

**Example Usage:**
```java
String type = CloudinaryUtil.getResourceType("video.mp4");
// Returns: "video"
```

## Integration with Forum System

### Post Creation Flow

1. User selects a file using the attachment button in the forum
2. File path is stored in the `attachedFilePath` variable
3. When the post is submitted, `CloudinaryUtil.uploadMedia()` is called
4. The returned secure URL is stored in the database with the post
5. The media is displayed in the post card using the secure URL

### Code Example (ForumController)

```java
String mediaUrl = "";
if (attachedFilePath != null && !attachedFilePath.isEmpty()) {
    System.out.println("Uploading media to Cloudinary...");
    String uploadedUrl = CloudinaryUtil.uploadMedia(attachedFilePath);
    if (uploadedUrl != null) {
        mediaUrl = uploadedUrl;
    } else {
        showError("Erreur lors de l'upload du fichier vers Cloudinary.");
        return;
    }
}
```

## Upload Process

1. **Validation:** Check if file path is valid and file exists
2. **Type Detection:** Determine resource type based on file extension
3. **Upload:** Send file to Cloudinary with appropriate resource type parameter
4. **Response Parsing:** Extract secure URL from upload response
5. **Return:** Return secure URL or null on failure

## Error Handling

The system handles various error scenarios:

| Error Scenario | Handling |
|----------------|----------|
| Null or empty file path | Returns `null` |
| File does not exist | Logs error, returns `null` |
| Network issues during upload | Catches IOException, logs stack trace, returns `null` |
| Invalid Cloudinary credentials | Throws IOException with details |

## Logging

The system provides detailed logging for debugging:

- Upload start message with file type
- Success message with secure URL
- Error messages with exception details

Example log output:
```
Uploading image file: C:/Users/John/Desktop/image.jpg
Upload successful: https://res.cloudinary.com/dlhz8gtag/image/upload/...
```

## Database Schema

Media URLs are stored in the `posts` table:

```sql
media_url VARCHAR(500)  -- Stores the Cloudinary secure URL
media_type VARCHAR(50)  -- Stores the media type (image/video/audio)
```

## Security Considerations

1. **API Key Protection:** Cloudinary credentials should be moved to environment variables
2. **File Size Limits:** Consider implementing file size limits to prevent abuse
3. **File Type Validation:** Add server-side validation to ensure only allowed file types are uploaded
4. **URL Validation:** Validate that returned URLs are from the correct Cloudinary domain

## Future Enhancements

Potential improvements to the system:

1. **Asynchronous Upload:** Implement async upload to prevent blocking the UI
2. **Progress Tracking:** Add upload progress indicators
3. **Image Optimization:** Use Cloudinary transformations to optimize images
4. **Video Transcoding:** Use Cloudinary to transcode videos to optimal formats
5. **CDN Caching:** Leverage Cloudinary's CDN for faster content delivery
6. **Backup URLs:** Store backup URLs in case of CDN issues

## Troubleshooting

### Common Issues

**Issue:** Upload returns `null`
- **Solution:** Check file path, ensure file exists, verify Cloudinary credentials

**Issue:** "File does not exist" error
- **Solution:** Verify the file path is correct and accessible

**Issue:** Network timeout during upload
- **Solution:** Check internet connection, increase timeout values

**Issue:** Invalid resource type
- **Solution:** Ensure file extension matches supported formats

## Dependencies

The system requires the Cloudinary Java SDK:

```xml
<dependency>
    <groupId>com.cloudinary</groupId>
    <artifactId>cloudinary-http44</artifactId>
    <version>1.x.x</version>
</dependency>
```

## Related Files

- `CloudinaryUtil.java` - Main utility class
- `ForumController.java` - Integration with post creation
- `post_like.sql` - Database schema for posts with media
