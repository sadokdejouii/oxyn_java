# Translation System

## Overview

The Translation System is a utility class (`TranslationUtil`) that provides automatic language detection and translation capabilities using the MyMemory Translation API. This system enables the forum application to translate posts and comments from various languages into English, making content accessible to a global audience.

## Architecture

### Main Class: `TranslationUtil`

**Location:** `src/main/java/org/example/utils/TranslationUtil.java`

The utility class uses pattern matching for language detection and the MyMemory Translation API for actual translation.

## Configuration

### API Configuration

- **API Endpoint:** `https://api.mymemory.translated.net/get`
- **Max Text Length:** 500 characters
- **Timeout:** 10 seconds (connect), 10 seconds (read)

### Supported Languages

The system supports detection and translation for the following languages:

| Language Code | Language Name | Detection Method |
|---------------|---------------|------------------|
| `en` | English | Default, character patterns |
| `fr` | French | Accent marks, common words |
| `es` | Spanish | Special characters, common words |
| `de` | German | Umlauts, common words |
| `it` | Italian | Common words, patterns |
| `pt` | Portuguese | Special characters, common words |
| `ar` | Arabic | Arabic Unicode range |
| `zh` | Chinese | Chinese Unicode range |
| `ru` | Russian | Cyrillic Unicode range |
| `el` | Greek | Greek Unicode range |

## Features

### 1. Automatic Language Detection

The system uses character pattern analysis and keyword matching to detect the source language:

- **Unicode Range Detection:** Identifies languages by their character ranges (Arabic, Chinese, Russian, Greek)
- **Accent Detection:** Identifies languages by specific accent marks (French, German, Spanish, Portuguese)
- **Keyword Matching:** Identifies languages by common words and phrases

### 2. Translation to English

The primary use case is translating text to English automatically:
- Detects source language
- Skips translation if already English
- Translates using MyMemory API
- Returns original text on error

### 3. Bidirectional Translation

Supports translation between any supported language pair:
- Source language detection or manual specification
- Target language specification
- Language pair validation

## API Methods

### `detectLanguage(String text)`

**Purpose:** Detects the language of given text using character pattern analysis.

**Parameters:**
- `text` (String): The text to analyze

**Returns:**
- `String`: ISO 639-1 language code (e.g., "en", "fr", "es")

**Example Usage:**
```java
String language = TranslationUtil.detectLanguage("Bonjour, comment allez-vous?");
// Returns: "fr"
```

**Detection Logic:**
1. Checks for Arabic characters (U+0600 to U+06FF)
2. Checks for Chinese characters (U+4E00 to U+9FFF)
3. Checks for Cyrillic characters (U+0400 to U+04FF)
4. Checks for Greek characters (U+0370 to U+03FF)
5. Checks for Italian-specific keywords
6. Checks for French-specific patterns and accents
7. Checks for Spanish-specific patterns
8. Checks for German-specific patterns
9. Checks for Portuguese-specific patterns
10. Defaults to English

### `translateToEnglish(String text)`

**Purpose:** Translates text to English with automatic language detection.

**Parameters:**
- `text` (String): The text to translate

**Returns:**
- `String`: Translated text in English, or original text if translation fails

**Example Usage:**
```java
String translated = TranslationUtil.translateToEnglish("Hola, ¿cómo estás?");
// Returns: "Hello, how are you?"
```

**Behavior:**
- Truncates text to 500 characters if too long
- Detects source language automatically
- Skips translation if already English
- Returns original text on API error

### `translate(String text, String sourceLang, String targetLang)`

**Purpose:** Translates text from source language to target language.

**Parameters:**
- `text` (String): The text to translate
- `sourceLang` (String): Source language code (e.g., "en", "fr", "es")
- `targetLang` (String): Target language code (e.g., "en", "fr", "es")

**Returns:**
- `String`: Translated text, or original text if translation fails

**Example Usage:**
```java
String translated = TranslationUtil.translate("Bonjour", "fr", "en");
// Returns: "Hello"
```

**Behavior:**
- Defaults source and target to "en" if null/empty
- Returns text as-is if source equals target
- Truncates to 500 characters
- Makes HTTP request to MyMemory API
- Parses JSON response to extract translated text
- Returns original text on error

### `isEnglish(String text)`

**Purpose:** Checks if the given text is likely in English.

**Parameters:**
- `text` (String): The text to check

**Returns:**
- `boolean`: `true` if English, `false` otherwise

**Example Usage:**
```java
boolean english = TranslationUtil.isEnglish("Hello world");
// Returns: true
```

### `getLanguageName(String langCode)`

**Purpose:** Gets the full language name from ISO code.

**Parameters:**
- `langCode` (String): ISO 639-1 language code

**Returns:**
- `String`: Full language name (e.g., "English", "French")

**Example Usage:**
```java
String name = TranslationUtil.getLanguageName("fr");
// Returns: "French"
```

## Integration with Forum System

### Post Translation Flow

1. User creates a post in any language
2. The system detects the language automatically
3. If not English, the post is translated
4. Both original and translated versions can be stored/displayed

### Comment Translation Flow

1. User adds a comment in any language
2. The system detects the language
3. If not English, the comment is translated
4. Translation is displayed to users who prefer English

### Code Example (ForumController)

```java
// Translate post content to English
String translatedContent = TranslationUtil.translateToEnglish(post.getContent_post());
```

## API Request Format

### Endpoint

```
GET https://api.mymemory.translated.net/get?q={text}&langpair={source}|{target}
```

### Parameters

- `q`: URL-encoded text to translate
- `langpair`: Language pair in format "source|target"

### Response Format

```json
{
  "responseData": {
    "translatedText": "Translated text here",
    "match": 1.0
  },
  "responseStatus": 200,
  "responseDetails": "OK"
}
```

## JSON Parsing

The system includes a custom JSON parser to extract the translated text from the API response without external dependencies:

```java
private static String extractTranslatedText(String jsonResponse) {
    // Looks for "translatedText":"..." pattern
    // Handles escaped characters
    // Returns null if not found
}
```

## Error Handling

The system handles various error scenarios:

| Error Scenario | Handling |
|----------------|----------|
| Null or empty text | Returns text as-is |
| Text exceeds 500 characters | Truncates to 500 characters |
| API timeout (10s) | Returns original text |
| API returns non-200 status | Returns original text |
| Invalid language pair | Returns original text |
| JSON parsing error | Returns original text |
| Network error | Returns original text with stack trace |

## Logging

The system provides error logging for debugging:

```java
System.err.println("Translation error: " + e.getMessage());
System.err.println("Translation API returned status: " + responseCode);
System.err.println("Could not extract translated text from response");
```

## Language Detection Examples

### French
```java
TranslationUtil.detectLanguage("Bonjour, comment ça va?");
// Returns: "fr"
```

### Spanish
```java
TranslationUtil.detectLanguage("Hola, ¿cómo estás?");
// Returns: "es"
```

### German
```java
TranslationUtil.detectLanguage("Hallo, wie geht es dir?");
// Returns: "de"
```

### Arabic
```java
TranslationUtil.detectLanguage("مرحبا، كيف حالك؟");
// Returns: "ar"
```

### Chinese
```java
TranslationUtil.detectLanguage("你好，你好吗？");
// Returns: "zh"
```

### English
```java
TranslationUtil.detectLanguage("Hello, how are you?");
// Returns: "en"
```

## Translation Examples

### French to English
```java
TranslationUtil.translate("Bonjour le monde", "fr", "en");
// Returns: "Hello world"
```

### Spanish to English
```java
TranslationUtil.translate("Buenos días", "es", "en");
// Returns: "Good morning"
```

### German to English
```java
TranslationUtil.translate("Guten Tag", "de", "en");
// Returns: "Good day"
```

## Performance Considerations

1. **Text Length Limit:** 500 characters per request to prevent API abuse
2. **Timeout:** 10-second timeout to prevent hanging
3. **No External Dependencies:** Custom JSON parsing avoids library overhead
4. **Caching:** Consider adding caching for frequently translated phrases

## Security Considerations

1. **API Rate Limits:** MyMemory has rate limits; consider implementing request throttling
2. **Input Sanitization:** Text is URL-encoded before sending to API
3. **Error Messages:** Error details are logged but not exposed to end users
4. **Content Filtering:** Consider adding content filtering before translation

## Future Enhancements

Potential improvements to the system:

1. **Batch Translation:** Support translating multiple texts in one request
2. **Caching Layer:** Add caching to reduce API calls
3. **Fallback APIs:** Integrate alternative translation APIs (Google, DeepL)
4. **Language Preference:** Store user language preferences
5. **Real-time Translation:** Implement real-time translation as user types
6. **Confidence Scores:** Display translation confidence scores
7. **Context-aware Translation:** Use context for better translation quality

## Troubleshooting

### Common Issues

**Issue:** Translation returns original text
- **Solution:** Check if text is already English, verify API connectivity

**Issue:** "Could not extract translated text" error
- **Solution:** API response format may have changed, check response body

**Issue:** Language detection incorrect
- **Solution:** Improve keyword patterns for specific languages

**Issue:** Timeout errors
- **Solution:** Increase timeout values or check network connectivity

## Dependencies

The system has no external dependencies - it uses standard Java libraries:
- `java.net.*` for HTTP requests
- `java.util.regex.*` for pattern matching
- `java.nio.charset.*` for encoding

## Related Files

- `TranslationUtil.java` - Main utility class
- `ForumController.java` - Integration with post/comment translation
- `CohereSummarizer.java` - Often used with translation for summarization

## API Limitations

MyMemory Translation API (free tier) limitations:
- 500 characters per request
- Rate limits (approximately 10 requests per hour for anonymous users)
- No guarantee of service availability
- Translation quality varies by language pair

## Best Practices

1. **Always check if text is already in target language before translating**
2. **Handle null and empty text gracefully**
3. **Log errors for debugging but don't expose to users**
4. **Implement retry logic for transient failures**
5. **Consider caching translations to reduce API calls**
6. **Validate language codes before use**
