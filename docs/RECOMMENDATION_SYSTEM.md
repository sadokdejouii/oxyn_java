# Recommendation System

## Overview

The Recommendation System is a service (`RecommendationService`) that tracks user interactions with different content categories and calculates personalized recommendations based on user preferences. This system enhances user engagement by suggesting relevant content based on their activity patterns.

## Architecture

### Main Class: `RecommendationService`

**Location:** `src/main/java/org/example/services/RecommendationService.java`

The service manages user recommendation data including category scores, interaction counts, and personalized content suggestions.

### Entity: `UserRecommendation`

**Location:** `src/main/java/org/example/entities/UserRecommendation.java`

Represents a user's recommendation profile with the following fields:
- `id` - Primary key
- `userId` - User ID (foreign key)
- `categoryScoresJson` - JSON object mapping categories to scores
- `totalInteractions` - Total number of interactions across all categories
- `lastUpdated` - Timestamp of last update

## Database Schema

### Table: `user_recommendation`

```sql
CREATE TABLE user_recommendation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    category_scores_json JSON DEFAULT '{}',
    total_interactions INT DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id_user) ON DELETE CASCADE
);
```

## Features

### 1. Category Interaction Tracking

Users earn scores for interacting with content in different categories:

| Action Type | Score Weight | Description |
|-------------|--------------|-------------|
| CREATE_POST | 5 | Creating posts shows strong interest |
| ADD_COMMENT | 3 | Commenting shows engagement |
| LIKE_POST | 2 | Liking shows mild interest |
| VIEW_POST | 1 | Viewing shows basic interest |

### 2. Score Calculation

Category scores are cumulative and weighted by interaction type:
- Higher weight actions contribute more to category preference
- Scores persist across sessions
- Total interactions track overall engagement

### 3. Personalized Recommendations

The system recommends categories and posts based on:
- User's historical category scores
- Interaction patterns
- Default categories for new users

### 4. Default Categories

For users with no interaction history, default categories are:
- Général
- Technology
- Sports
- Music
- Art
- Science
- Business
- Entertainment

## API Methods

### `createRecommendationForUser(int userId)`

**Purpose:** Creates a new recommendation record for a user.

**Parameters:**
- `userId` (int): The ID of the user

**Throws:**
- `SQLException` if database operation fails

**Initial Values:**
- Category scores: Empty object `{}`
- Total interactions: 0

**Example Usage:**
```java
recommendationService.createRecommendationForUser(userId);
```

### `getRecommendationByUserId(int userId)`

**Purpose:** Retrieves recommendation data for a user.

**Parameters:**
- `userId` (int): The ID of the user

**Returns:**
- `UserRecommendation` object or null if not found

**Throws:**
- `SQLException` if database operation fails

**Example Usage:**
```java
UserRecommendation recommendation = recommendationService.getRecommendationByUserId(userId);
```

### `handleUserAction(int userId, String category, ActionType actionType)`

**Purpose:** Main entry point for updating recommendations based on user actions.

**Parameters:**
- `userId` (int): The ID of the user performing the action
- `category` (String): The category of the content
- `actionType` (ActionType): The type of action performed

**Throws:**
- `SQLException` if database operation fails

**Action Types:**
- `CREATE_POST` - User created a post in a category
- `ADD_COMMENT` - User commented on a post in a category
- `LIKE_POST` - User liked a post in a category
- `VIEW_POST` - User viewed a post in a category

**Behavior:**
1. Retrieves or creates recommendation record for user
2. Increments category score based on action type weight
3. Increments total interactions count
4. Updates last updated timestamp
5. Saves to database

**Example Usage:**
```java
recommendationService.handleUserAction(userId, "Nutrition", 
                                      RecommendationService.ActionType.LIKE_POST);
```

### `updateRecommendation(UserRecommendation recommendation)`

**Purpose:** Updates recommendation record in database.

**Parameters:**
- `recommendation` (UserRecommendation): The UserRecommendation object to update

**Throws:**
- `SQLException` if database operation fails

### `getRecommendedCategories(int userId, int limit)`

**Purpose:** Gets recommended categories for a user based on their interaction scores.

**Parameters:**
- `userId` (int): The ID of the user
- `limit` (int): Maximum number of categories to return

**Returns:**
- `List<String>` of recommended category names

**Throws:**
- `SQLException` if database operation fails

**Behavior:**
- Returns top categories by score for users with interactions
- Returns default categories for new users
- Sorted by score in descending order

**Example Usage:**
```java
List<String> categories = recommendationService.getRecommendedCategories(userId, 5);
```

### `getDefaultCategories(int limit)`

**Purpose:** Gets default categories for new users.

**Parameters:**
- `limit` (int): Maximum number of categories to return

**Returns:**
- `List<String>` of default category names

### `getAllCategories()`

**Purpose:** Gets all available categories from posts.

**Returns:**
- `List<String>` of distinct category names

**Throws:**
- `SQLException` if database operation fails

### `getCategoryScore(int userId, String category)`

**Purpose:** Gets category score for a specific user and category.

**Parameters:**
- `userId` (int): The ID of the user
- `category` (String): The category name

**Returns:**
- `int` - The category score (0 if not found)

**Throws:**
- `SQLException` if database operation fails

### `resetRecommendation(int userId)`

**Purpose:** Resets recommendation data for a user.

**Parameters:**
- `userId` (int): The ID of the user

**Throws:**
- `SQLException` if database operation fails

**Behavior:**
- Sets category scores to empty object
- Sets total interactions to 0

### `deleteRecommendation(int userId)`

**Purpose:** Deletes recommendation record for a user.

**Parameters:**
- `userId` (int): The ID of the user

**Throws:**
- `SQLException` if database operation fails

### `getTopUsersByCategory(String category, int limit)`

**Purpose:** Gets users with highest interaction in a specific category.

**Parameters:**
- `category` (String): The category name
- `limit` (int): Maximum number of users to return

**Returns:**
- `List<Map<String, Object>>` containing user data and category scores

**Throws:**
- `SQLException` if database operation fails

### `getRecommendedPosts(int userId, int limit)`

**Purpose:** Gets personalized feed recommendations for a user.

**Parameters:**
- `userId` (int): The ID of the user
- `limit` (int): Maximum number of posts to return

**Returns:**
- `List<Integer>` of post IDs

**Throws:**
- `SQLException` if database operation fails

**Behavior:**
- Returns posts from recommended categories
- Posts are ordered by category preference
- Returns random posts if no preferences

**Example Usage:**
```java
List<Integer> postIds = recommendationService.getRecommendedPosts(userId, 10);
```

## Integration with Forum System

### Post Creation Flow

1. User creates a post in a category
2. `handleUserAction(userId, category, CREATE_POST)` is called
3. Category score increased by 5
4. Total interactions increased by 1
5. Database updated
6. Future recommendations reflect this preference

### Comment Creation Flow

1. User comments on a post in a category
2. `handleUserAction(userId, category, ADD_COMMENT)` is called
3. Category score increased by 3
4. Total interactions increased by 1
5. Database updated

### Like Action Flow

1. User likes a post in a category
2. `handleUserAction(userId, category, LIKE_POST)` is called
3. Category score increased by 2
4. Total interactions increased by 1
5. Database updated

### Post View Flow

1. User views a post in a category
2. `handleUserAction(userId, category, VIEW_POST)` is called
3. Category score increased by 1
4. Total interactions increased by 1
5. Database updated

### Code Example (ForumController)

```java
// When user creates a post
recommendationService.handleUserAction(currentUserId, post.getCategory_post(),
                                      RecommendationService.ActionType.CREATE_POST);

// When user adds a comment
recommendationService.handleUserAction(currentUserId, post.getCategory_post(),
                                      RecommendationService.ActionType.ADD_COMMENT);

// When user likes a post
recommendationService.handleUserAction(currentUserId, post.getCategory_post(),
                                      RecommendationService.ActionType.LIKE_POST);

// Get recommended posts for user
List<Integer> recommendedPostIds = recommendationService.getRecommendedPosts(userId, 10);
```

## Score Calculation Logic

### Score Weights

```java
private static final Map<ActionType, Integer> SCORE_WEIGHTS = new HashMap<>();
static {
    SCORE_WEIGHTS.put(ActionType.CREATE_POST, 5);
    SCORE_WEIGHTS.put(ActionType.ADD_COMMENT, 3);
    SCORE_WEIGHTS.put(ActionType.LIKE_POST, 2);
    SCORE_WEIGHTS.put(ActionType.VIEW_POST, 1);
}
```

### Category Score Increment

```java
int scoreIncrement = SCORE_WEIGHTS.getOrDefault(actionType, 1);
recommendation.incrementCategoryScore(category, scoreIncrement);
```

## Category Scores JSON Format

The category scores are stored as a JSON object:

```json
{
  "Nutrition": 25,
  "Sports": 18,
  "Technology": 12,
  "Music": 8
}
```

## Recommendation Algorithm

### Top Categories Selection

1. Retrieve user's category scores
2. Sort categories by score in descending order
3. Return top N categories

### Post Recommendation

1. Get user's recommended categories
2. Query posts from those categories
3. Order posts by category preference (FIELD clause)
4. Return top N posts

### SQL Query for Post Recommendation

```sql
SELECT id_post, category_post 
FROM post 
WHERE category_post IN (?, ?, ?, ?, ?)
ORDER BY FIELD(category_post, ?, ?, ?, ?, ?)
LIMIT ?
```

## Error Handling

The system handles various error scenarios:

| Error Scenario | Handling |
|----------------|----------|
| User has no recommendation record | Creates record automatically |
| Invalid category name | Adds to scores with default weight |
| Database connection failure | Throws SQLException |
| No posts in recommended categories | Returns random posts |

## Performance Considerations

1. **Database Indexes:** Ensure user_id is indexed for fast lookups
2. **JSON Parsing:** Category scores stored as JSON for flexibility
3. **Caching:** Consider caching recommended posts for frequently accessed users
4. **Batch Updates:** Consider batching updates for high-volume scenarios
5. **Query Optimization:** Use FIELD clause for category ordering

## Security Considerations

1. **SQL Injection:** Uses PreparedStatement to prevent SQL injection
2. **User Validation:** Ensure user exists before creating recommendation record
3. **Score Manipulation:** Scores calculated server-side, cannot be forged
4. **Category Validation:** Consider validating category names against whitelist

## Future Enhancements

Potential improvements to the system:

1. **Time Decay:** Implement time-based score decay for old interactions
2. **Collaborative Filtering:** Add collaborative filtering based on similar users
3. **Content-Based Filtering:** Analyze post content for better recommendations
4. **Hybrid Approach:** Combine multiple recommendation algorithms
5. **Real-time Updates:** WebSocket-based real-time recommendation updates
6. **A/B Testing:** Test different recommendation strategies
7. **Cold Start Solutions:** Better recommendations for new users
8. **Explainable AI:** Show users why posts are recommended
9. **Multi-armed Bandit:** Optimize recommendation strategy over time
10. **Category Hierarchies:** Support hierarchical category structures

## Troubleshooting

### Common Issues

**Issue:** Recommendation record not found for user
- **Solution:** System auto-creates record on first action

**Issue:** No recommended posts returned
- **Solution:** Check if posts exist in recommended categories

**Issue:** Category scores not updating
- **Solution:** Verify `handleUserAction()` is being called after user actions

**Issue:** Recommendations not reflecting recent activity
- **Solution:** Check that `lastUpdated` timestamp is being set correctly

**Issue:** Random posts returned instead of recommendations
- **Solution:** User may have no interaction history, system returns defaults

## Dependencies

The system requires:
- JDBC for database connectivity
- JSON library for score storage
- `MyDataBase` for database connection management

## Related Files

- `RecommendationService.java` - Main service class
- `UserRecommendation.java` - Entity class
- `ForumController.java` - Integration with forum actions
- `user_recommendation.sql` - Database schema

## Best Practices

1. **Always call handleUserAction() after user interactions**
2. **Handle SQLExceptions gracefully in controllers**
3. **Use recommended categories to filter content**
4. **Display recommended posts prominently**
5. **Monitor recommendation accuracy**
6. **Regularly review score weights**
7. **Consider user feedback on recommendations**
8. **Track recommendation click-through rates**

## Example Workflow

```java
// User Registration
public void registerUser(User user) {
    userService.save(user);
    recommendationService.createRecommendationForUser(user.getId());
}

// Post Creation
public void createPost(Post post) {
    postService.save(post);
    recommendationService.handleUserAction(post.getAuthorId(), 
                                          post.getCategory_post(),
                                          RecommendationService.ActionType.CREATE_POST);
}

// Display User Feed
public void displayUserFeed(int userId) {
    List<Integer> postIds = recommendationService.getRecommendedPosts(userId, 20);
    List<Post> posts = postService.getPostsByIds(postIds);
    // Display posts to user
}

// Display Category Suggestions
public void displayCategorySuggestions(int userId) {
    List<String> categories = recommendationService.getRecommendedCategories(userId, 5);
    // Display category buttons or filters
}
```

## Analytics Metrics

Recommended metrics to track:
- Total interactions per user
- Category score distribution
- Recommendation click-through rate
- Time spent on recommended content
- User engagement by recommended vs non-recommended content
- Category popularity trends
- Recommendation accuracy
- Cold start user behavior
- Score weight effectiveness

## Algorithm Comparison

### Current Algorithm: Weighted Score

**Pros:**
- Simple to implement
- Fast computation
- Transparent to users
- Easy to debug

**Cons:**
- Doesn't account for recency
- No collaborative filtering
- Limited personalization depth

### Alternative Algorithms

1. **Collaborative Filtering**
   - Pros: Finds similar users, can recommend new categories
   - Cons: Cold start problem, computationally expensive

2. **Content-Based Filtering**
   - Pros: No cold start problem, transparent
   - Cons: Limited to existing interests, no serendipity

3. **Matrix Factorization**
   - Pros: Highly accurate, handles sparse data
   - Cons: Complex, computationally expensive

4. **Deep Learning**
   - Pros: Can capture complex patterns
   - Cons: Requires large dataset, hard to interpret

## Configuration Options

Consider making these configurable:

```java
// Score weights
private static final int CREATE_POST_WEIGHT = 5;
private static final int ADD_COMMENT_WEIGHT = 3;
private static final int LIKE_POST_WEIGHT = 2;
private static final int VIEW_POST_WEIGHT = 1;

// Time decay factor (future)
private static final double TIME_DECAY_FACTOR = 0.95;

// Recommendation limits
private static final int DEFAULT_CATEGORY_LIMIT = 5;
private static final int DEFAULT_POST_LIMIT = 10;
```

## Testing Considerations

1. **Unit Tests:** Test score calculation and category ranking
2. **Integration Tests:** Test database operations
3. **Performance Tests:** Test with large datasets
4. **A/B Tests:** Compare recommendation strategies
5. **User Testing:** Gather feedback on recommendation quality
