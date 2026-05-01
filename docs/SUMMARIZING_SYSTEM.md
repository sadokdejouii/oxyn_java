# Summarizing System

## Overview

The Summarizing System is a utility class (`CohereSummarizer`) that provides AI-powered text summarization using the Cohere AI API. This system enables the forum application to automatically generate concise summaries of long posts, improving content readability and user experience.

## Architecture

### Main Class: `CohereSummarizer`

**Location:** `src/main/java/org/example/utils/CohereSummarizer.java`

The utility class uses the Cohere AI API (v2 with v1 fallback) to generate summaries without external dependencies.

## Configuration

### API Configuration

- **API Key:** `77hxudBQaNzIwtgizELsVUDkcnzhxhYxC1I7Ewcu`
- **API URL v2:** `https://api.cohere.com/v2/chat`
- **API URL v1:** `https://api.cohere.com/v1/chat` (fallback)
- **Model:** `command-r-08-2024`
- **Connect Timeout:** 10,000ms (10 seconds)
- **Read Timeout:** 30,000ms (30 seconds)
- **Max Retries:** 3
- **Retry Delay:** 1,000ms (1 second)

> **Security Note:** In a production environment, the API key should be stored in environment variables or a secure configuration file.

## Features

### 1. Automatic API Version Handling

The system automatically tries the v2 API first and falls back to v1 on 404 errors, ensuring compatibility during API transitions.

### 2. Retry Logic

Implements automatic retry with exponential backoff for transient failures:
- Up to 3 retries
- 1-second delay between retries
- Handles rate limiting (429) and server errors (5xx)

### 3. JSON Parsing Without Dependencies

Custom JSON parsing handles both v2 and v1 API response formats without requiring external JSON libraries.

### 4. Error Handling

Comprehensive error handling with detailed logging for debugging.

## API Methods

### `summarize(String text)`

**Purpose:** Generates a summary of the given text using Cohere AI.

**Parameters:**
- `text` (String): The text to summarize

**Returns:**
- `String`: Summary of the text (2-3 sentences), or original text if summarization fails

**Behavior:**
- Returns text as-is if null, empty, or less than 50 characters
- Attempts up to 3 retries on failure
- Returns original text if all attempts fail
- Logs warnings for debugging

**Example Usage:**
```java
String longText = "This is a very long text that needs to be summarized...";
String summary = CohereSummarizer.summarize(longText);
System.out.println(summary);
```

### Internal Methods

#### `sendRequest(String text)`

Handles API request with automatic v2 to v1 fallback on 404 errors.

#### `sendToEndpoint(String apiUrl, String jsonBody)`

Sends HTTP POST request to specified endpoint with error handling.

#### `buildV2Body(String text)`

Builds JSON request body for v2 API (messages array format).

#### `buildV1Body(String text)`

Builds JSON request body for v1 API (flat message format).

#### `extractText(String json)`

Extracts summary text from JSON response, handling both v2 and v1 formats.

#### `extractStringValue(String json, int keyIndex)`

Helper method to extract string values from JSON with proper escape handling.

#### `escapeJson(String text)`

Escapes special characters for JSON serialization.

#### `readStream(HttpURLConnection conn, boolean errorStream)`

Reads HTTP response stream as string.

## Integration with Forum System

### Post Summarization Flow

1. User creates a long post
2. The system checks if post length exceeds threshold
3. If so, `CohereSummarizer.summarize()` is called
4. Summary is stored alongside the full post
5. Summary is displayed in post previews and cards

### Code Example (ForumController)

```java
// Generate summary for long posts
String summary = post.getContent_post();
if (summary.length() > 200) {
    summary = CohereSummarizer.summarize(post.getContent_post());
}
```

## API Request Format

### v2 API Request

```json
{
  "model": "command-r-08-2024",
  "messages": [
    {
      "role": "user",
      "content": "Summarize the following text in 2-3 concise sentences: [text]"
    }
  ]
}
```

### v1 API Request

```json
{
  "model": "command-r-08-2024",
  "message": "Summarize the following text in 2-3 concise sentences: [text]"
}
```

## API Response Format

### v2 Response

```json
{
  "message": {
    "content": [
      {
        "type": "text",
        "text": "The summary text here"
      }
    ]
  }
}
```

### v1 Response

```json
{
  "text": "The summary text here",
  ...
}
```

## JSON Parsing

The system uses custom JSON parsing to handle both response formats:

```java
private static String extractText(String json) {
    // Try v2 format: message -> content[] -> text
    // Fallback to v1 format: top-level text field
    // Returns null if parsing fails
}
```

## Error Handling

The system handles various error scenarios:

| Error Scenario | Handling |
|----------------|----------|
| Null or empty text | Returns text as-is |
| Text < 50 characters | Returns text as-is |
| HTTP 404 (v2) | Retries with v1 endpoint |
| HTTP 429 (rate limit) | Retries up to 3 times |
| HTTP 5xx (server error) | Retries up to 3 times |
| HTTP timeout | Retries up to 3 times |
| All retries failed | Returns original text |
| JSON parsing error | Returns original text |

## Logging

The system uses Java's built-in logging for debugging:

```java
LOGGER.log(Level.WARNING, "Summarization attempt " + (attempt + 1) + " failed: " + e.getMessage());
LOGGER.log(Level.WARNING, "v2 returned 404 – retrying with v1 endpoint");
LOGGER.log(Level.WARNING, "HTTP " + code + " from " + apiUrl + " | error body: " + errBody);
LOGGER.log(Level.WARNING, "All summarization attempts failed, returning original text");
```

## Retry Strategy

### Retry Conditions

- Rate limiting (HTTP 429)
- Server errors (HTTP 5xx)
- Network timeouts
- v2 API 404 (triggers v1 fallback)

### Retry Logic

```java
for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
    try {
        String summary = sendRequest(text);
        if (summary != null && !summary.trim().isEmpty()) return summary;
    } catch (IOException e) {
        if (attempt < MAX_RETRIES - 1) {
            Thread.sleep(RETRY_DELAY_MS);
        }
    }
}
```

## Performance Considerations

1. **Text Length Threshold:** Only summarizes text longer than 50 characters
2. **Timeout:** 30-second read timeout to prevent hanging
3. **Retry Limit:** Maximum 3 retries to prevent excessive delays
4. **No External Dependencies:** Custom JSON parsing avoids library overhead
5. **Connection Pooling:** Consider implementing connection pooling for high volume

## Security Considerations

1. **API Key Protection:** API key should be moved to environment variables
2. **Input Validation:** Text is JSON-escaped before sending to API
3. **Output Sanitization:** Consider sanitizing summary output
4. **Rate Limiting:** Implement client-side rate limiting to avoid API abuse
5. **Content Filtering:** Consider adding content filtering before summarization

## Future Enhancements

Potential improvements to the system:

1. **Configurable Summary Length:** Allow users to specify summary length
2. **Caching Layer:** Add caching to reduce API calls for repeated content
3. **Batch Summarization:** Support summarizing multiple texts in one request
4. **Alternative APIs:** Integrate other summarization APIs (OpenAI, Anthropic)
5. **Streaming Responses:** Implement streaming for real-time summary generation
6. **Custom Prompts:** Allow custom summarization prompts
7. **Summary Quality Metrics:** Add metrics to evaluate summary quality
8. **Async Processing:** Implement async summarization to prevent blocking

## Troubleshooting

### Common Issues

**Issue:** Summarization returns original text
- **Solution:** Check if text is > 50 characters, verify API key is valid

**Issue:** "v2 returned 404" warnings
- **Solution:** Normal behavior, system falls back to v1 automatically

**Issue:** "All summarization attempts failed"
- **Solution:** Check network connectivity, verify API key, check API status

**Issue:** Timeout errors
- **Solution:** Increase timeout values or check network latency

**Issue:** "Could not parse text from response"
- **Solution:** API response format may have changed, check response body

## Dependencies

The system has no external dependencies - it uses standard Java libraries:
- `java.net.*` for HTTP requests
- `java.util.logging.*` for logging
- `java.io.*` for stream handling

## Related Files

- `CohereSummarizer.java` - Main utility class
- `ForumController.java` - Integration with post summarization
- `TranslationUtil.java` - Often used with summarization for multi-language support

## API Limitations

Cohere AI API limitations (free tier):
- Rate limits (number of requests per minute/hour)
- Token limits per request
- Model availability
- No guarantee of service availability
- Summary quality varies by content type

## Best Practices

1. **Always check text length before summarizing**
2. **Handle null and empty text gracefully**
3. **Implement proper error handling and logging**
4. **Use retry logic for transient failures**
5. **Consider caching summaries to reduce API calls**
6. **Validate API responses before processing**
7. **Monitor API usage to stay within limits**

## Example Workflow

```java
// Example: Summarize a long forum post
String postContent = "This is a very long post about fitness training...";
if (postContent.length() > 200) {
    String summary = CohereSummarizer.summarize(postContent);
    // Store summary in database
    post.setSummary(summary);
    postService.update(post);
}
```

## Summary Quality

The system is configured to generate summaries of 2-3 concise sentences. This provides:
- Quick overview of content
- Reduced reading time for users
- Better mobile experience
- Improved searchability

## Cost Considerations

Cohere AI API usage costs:
- Charged per token processed
- Longer texts cost more to summarize
- Consider implementing length limits to control costs
- Caching can significantly reduce costs

## Monitoring

Recommended monitoring metrics:
- Number of summarization requests
- Average request duration
- Error rates by type
- API response times
- Cache hit/miss ratios (if caching implemented)
