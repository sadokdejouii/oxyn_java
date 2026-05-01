# Gamification System

## Overview

The Gamification System is a comprehensive service (`GamificationService`) that manages user engagement through points, levels, badges, and leaderboards. This system incentivizes user participation in the forum by rewarding various activities with points and achievements.

## Architecture

### Main Class: `GamificationService`

**Location:** `src/main/java/org/example/services/GamificationService.java`

The service manages user gamification data including points, levels, badges, and statistics.

### Entity: `UserGamification`

**Location:** `src/main/java/org/example/entities/UserGamification.java`

Represents a user's gamification profile with the following fields:
- `id` - Primary key
- `userId` - User ID (foreign key)
- `points` - Total points earned
- `level` - Current level (Beginner, Intermediate, Advanced)
- `badgesJson` - JSON array of earned badges
- `postsCount` - Number of posts created
- `commentsCount` - Number of comments added
- `likesReceived` - Number of likes received on posts

## Database Schema

### Table: `user_gamification`

```sql
CREATE TABLE user_gamification (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    points INT DEFAULT 0,
    level VARCHAR(50) DEFAULT 'Beginner',
    badges_json JSON DEFAULT '[]',
    posts_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    likes_received INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id_user) ON DELETE CASCADE
);
```

## Features

### 1. Point System

Users earn points for various actions:

| Action | Points |
|--------|--------|
| Create post | +5 |
| Add comment | +2 |
| Receive like | +1 |

### 2. Level System

Users progress through levels based on total points:

| Points Range | Level |
|--------------|-------|
| 0-30 | Beginner |
| 31-80 | Intermediate |
| 81+ | Advanced |

### 3. Badge System

Users earn badges based on their achievements. Only the highest priority badge is displayed.

#### Badge Priority (Highest to Lowest)

1. **Superstar** - Points >= 100
2. **Beloved** - Likes received >= 50
3. **Content Creator** - Posts count >= 10
4. **Conversation Starter** - Comments count >= 15
5. **Rising Star** - Posts count >= 5
6. **Liked** - Likes received >= 20
7. **Chatty** - Comments count >= 5
8. **First Post** - Posts count == 1 (removed after second post)

### 4. Leaderboard

Ranking system that displays top users by points.

## API Methods

### `createGamificationForUser(int userId)`

**Purpose:** Creates a new gamification record for a user.

**Parameters:**
- `userId` (int): The ID of the user

**Throws:**
- `SQLException` if database operation fails

**Example Usage:**
```java
gamificationService.createGamificationForUser(userId);
```

**Initial Values:**
- Points: 0
- Level: Beginner
- Badges: Empty array
- Posts count: 0
- Comments count: 0
- Likes received: 0

### `handleUserAction(int userId, ActionType actionType)`

**Purpose:** Main entry point for updating gamification based on user actions.

**Parameters:**
- `userId` (int): The ID of the user performing the action
- `actionType` (ActionType): The type of action performed

**Throws:**
- `SQLException` if database operation fails

**Action Types:**
- `CREATE_POST` - User created a post
- `ADD_COMMENT` - User added a comment
- `RECEIVE_LIKE` - User received a like on their post

**Behavior:**
1. Retrieves or creates gamification record for user
2. Updates points and stats based on action type
3. Updates level based on new points
4. Assigns badges based on new stats
5. Saves to database
6. Logs the update

**Example Usage:**
```java
gamificationService.handleUserAction(userId, GamificationService.ActionType.CREATE_POST);
```

### `updateLevel(UserGamification gamification)`

**Purpose:** Updates the user's level based on their points.

**Parameters:**
- `gamification` (UserGamification): The UserGamification object to update

**Behavior:**
- 0-30 points → Beginner
- 31-80 points → Intermediate
- 81+ points → Advanced

### `assignBadges(UserGamification gamification)`

**Purpose:** Assigns badges to the user based on their statistics.

**Parameters:**
- `gamification` (UserGamification): The UserGamification object to update

**Badge Assignment Logic:**
- Removes "First Post" if user has > 1 post
- Assigns "First Post" if exactly 1 post
- Assigns "Rising Star" if posts >= 5
- Assigns "Content Creator" if posts >= 10
- Assigns "Chatty" if comments >= 5
- Assigns "Conversation Starter" if comments >= 15
- Assigns "Liked" if likes received >= 20
- Assigns "Beloved" if likes received >= 50
- Assigns "Superstar" if points >= 100

### `getGamificationByUserId(int userId)`

**Purpose:** Retrieves gamification data for a specific user.

**Parameters:**
- `userId` (int): The ID of the user

**Returns:**
- `UserGamification` object or null if not found

**Throws:**
- `SQLException` if database operation fails

**Example Usage:**
```java
UserGamification gamification = gamificationService.getGamificationByUserId(userId);
```

### `updateGamification(UserGamification gamification)`

**Purpose:** Updates an existing gamification record in the database.

**Parameters:**
- `gamification` (UserGamification): The UserGamification object to update

**Throws:**
- `SQLException` if database operation fails

### `getLeaderboard(int limit)`

**Purpose:** Retrieves the leaderboard - top users sorted by points.

**Parameters:**
- `limit` (int): Maximum number of users to return

**Returns:**
- `List<UserGamification>` sorted by points in descending order

**Throws:**
- `SQLException` if database operation fails

**Example Usage:**
```java
List<UserGamification> leaderboard = gamificationService.getLeaderboard(10);
```

### `getLeaderboard()`

**Purpose:** Retrieves the leaderboard with default limit of 10 users.

**Returns:**
- `List<UserGamification>` sorted by points in descending order

**Throws:**
- `SQLException` if database operation fails

### `getHighestPriorityBadge(List<String> badges)`

**Purpose:** Gets the highest priority badge from a list of badges.

**Parameters:**
- `badges` (List<String>): List of badge names

**Returns:**
- The highest priority badge, or null if list is empty

**Priority Order:**
1. Superstar
2. Beloved
3. Content Creator
4. Conversation Starter
5. Rising Star
6. Liked
7. Chatty
8. First Post

### `getUserRank(int userId)`

**Purpose:** Gets the rank of a specific user on the leaderboard.

**Parameters:**
- `userId` (int): The ID of the user

**Returns:**
- The rank (1-based) or -1 if user not found

**Throws:**
- `SQLException` if database operation fails

**Example Usage:**
```java
int rank = gamificationService.getUserRank(userId);
```

### `deleteGamification(int userId)`

**Purpose:** Deletes a gamification record for a user.

**Parameters:**
- `userId` (int): The ID of the user

**Throws:**
- `SQLException` if database operation fails

### `getAllGamification()`

**Purpose:** Gets all gamification records (for admin purposes).

**Returns:**
- `List<UserGamification>` of all records sorted by points

**Throws:**
- `SQLException` if database operation fails

## Integration with Forum System

### Post Creation Flow

1. User creates a post
2. `handleUserAction(userId, CREATE_POST)` is called
3. Points increased by 5
4. Posts count increased by 1
5. Level updated if needed
6. Badges assigned if criteria met
7. Database updated

### Comment Creation Flow

1. User adds a comment
2. `handleUserAction(userId, ADD_COMMENT)` is called
3. Points increased by 2
4. Comments count increased by 1
5. Level updated if needed
6. Badges assigned if criteria met
7. Database updated

### Like Received Flow

1. User receives a like on their post
2. `handleUserAction(userId, RECEIVE_LIKE)` is called
3. Points increased by 1
4. Likes received increased by 1
5. Level updated if needed
6. Badges assigned if criteria met
7. Database updated

### Code Example (ForumController)

```java
// When user creates a post
gamificationService.handleUserAction(currentUserId, GamificationService.ActionType.CREATE_POST);

// When user adds a comment
gamificationService.handleUserAction(currentUserId, GamificationService.ActionType.ADD_COMMENT);

// When user receives a like
gamificationService.handleUserAction(postAuthorId, GamificationService.ActionType.RECEIVE_LIKE);
```

## Badge System Details

### Badge Descriptions

| Badge | Criteria | Description |
|-------|----------|-------------|
| First Post | postsCount == 1 | Awarded for the first post, removed after second post |
| Rising Star | postsCount >= 5 | Active contributor |
| Content Creator | postsCount >= 10 | Prolific content creator |
| Chatty | commentsCount >= 5 | Engaged commenter |
| Conversation Starter | commentsCount >= 15 | Sparks discussions |
| Liked | likesReceived >= 20 | Well-liked posts |
| Beloved | likesReceived >= 50 | Highly appreciated |
| Superstar | points >= 100 | Top-tier contributor |

### Badge Display

Only the highest priority badge is displayed to users. The system uses `getHighestPriorityBadge()` to determine which badge to show.

## Leaderboard System

### Query

```sql
SELECT ug.*, u.nom, u.prenom 
FROM user_gamification ug 
JOIN user u ON ug.user_id = u.id_user 
ORDER BY ug.points DESC 
LIMIT ?
```

### Features

- Shows top users by points
- Includes user names for display
- Configurable limit
- Real-time ranking

### Rank Calculation

```sql
SELECT COUNT(*) + 1 as rank 
FROM user_gamification 
WHERE points > (SELECT points FROM user_gamification WHERE user_id = ?)
```

## Logging

The system provides detailed logging for debugging:

```java
System.out.println("Gamification record created for user ID: " + userId);
System.out.println("Gamification updated for user ID: " + userId + 
                  " - Action: " + actionType + 
                  " - New Points: " + gamification.getPoints() +
                  " - Level: " + gamification.getLevel());
System.out.println("Badge 'First Post' assigned to user ID: " + gamification.getUserId());
System.out.println("Badge 'First Post' removed from user ID: " + gamification.getUserId());
```

## Error Handling

The system handles various error scenarios:

| Error Scenario | Handling |
|----------------|----------|
| User has no gamification record | Creates record automatically |
| Database connection failure | Throws SQLException |
| Invalid user ID | Returns null or throws SQLException |
| Concurrent updates | Database handles via transactions |

## Performance Considerations

1. **Database Indexes:** Ensure user_id is indexed for fast lookups
2. **Badge Calculation:** Badge assignment is O(1) per check
3. **Leaderboard Query:** Uses JOIN and ORDER BY, consider caching for large datasets
4. **Batch Updates:** Consider batching updates for high-volume scenarios

## Security Considerations

1. **SQL Injection:** Uses PreparedStatement to prevent SQL injection
2. **User Validation:** Ensure user exists before creating gamification record
3. **Point Fraud:** Consider adding validation to prevent point manipulation
4. **Badge Spoofing:** Badges are calculated server-side, cannot be forged

## Future Enhancements

Potential improvements to the system:

1. **Daily/Weekly Streaks:** Track consecutive days of activity
2. **Achievement Milestones:** Add more milestone badges
3. **Point Decay:** Implement point decay for inactive users
4. **Seasonal Leaderboards:** Time-based leaderboard competitions
5. **Badge Collections:** Allow users to collect and display multiple badges
6. **Point Multipliers:** Temporary multipliers for special events
7. **Team Gamification:** Group-based gamification
8. **Rewards System:** Redeem points for rewards
9. **Notification System:** Notify users of badge unlocks
10. **Analytics Dashboard:** Admin dashboard for gamification metrics

## Troubleshooting

### Common Issues

**Issue:** Gamification record not found for user
- **Solution:** System auto-creates record on first action

**Issue:** Badge not displaying correctly
- **Solution:** Check badge priority logic in `getHighestPriorityBadge()`

**Issue:** Leaderboard not updating
- **Solution:** Ensure `handleUserAction()` is being called after user actions

**Issue:** Points not incrementing
- **Solution:** Verify action type is correct and database update succeeds

**Issue:** Level not updating
- **Solution:** Check `updateLevel()` logic and point thresholds

## Dependencies

The system requires:
- JDBC for database connectivity
- JSON library for badge storage
- `MyDataBase` for database connection management

## Related Files

- `GamificationService.java` - Main service class
- `UserGamification.java` - Entity class
- `ForumController.java` - Integration with forum actions
- `user_gamification.sql` - Database schema

## Best Practices

1. **Always call handleUserAction() after user actions**
2. **Handle SQLExceptions gracefully in controllers**
3. **Display only the highest priority badge**
4. **Use leaderboard to encourage competition**
5. **Log gamification updates for debugging**
6. **Consider adding gamification to user registration flow**
7. **Monitor point distribution to prevent inflation**
8. **Regularly review badge criteria for balance**

## Example Workflow

```java
// User Registration
public void registerUser(User user) {
    userService.save(user);
    gamificationService.createGamificationForUser(user.getId());
}

// Post Creation
public void createPost(Post post) {
    postService.save(post);
    gamificationService.handleUserAction(post.getAuthorId(), 
                                       GamificationService.ActionType.CREATE_POST);
}

// Display User Profile
public void displayUserProfile(int userId) {
    UserGamification gamification = gamificationService.getGamificationByUserId(userId);
    String badge = gamificationService.getHighestPriorityBadge(gamification.getBadges());
    int rank = gamificationService.getUserRank(userId);
    // Display to user
}
```

## Analytics Metrics

Recommended metrics to track:
- Total points awarded
- Points per user distribution
- Badge completion rates
- Level progression rates
- Leaderboard turnover
- User retention by gamification engagement
- Most active users
- Badge popularity
