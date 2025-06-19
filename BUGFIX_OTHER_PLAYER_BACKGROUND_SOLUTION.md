# Bug Fix: Background Profile Other Player Selalu Default - SOLVED

## Problem Description
When opening another user's profile, the background displayed was always "default", even though the `activeBackground` field in the visited user's Firestore document had changed.

The logcat showed:
```
Player background loaded: default
Background applied for other player: default
```

## Root Cause Analysis
The issue was in the data access pattern in `OtherPlayerProfileActivity.java`. The code was trying to access the `activeBackground` field from a nested `user_profile` object:

```java
// INCORRECT - Looking for nested path
java.util.Map<String, Object> userProfile = (java.util.Map<String, Object>) document.get("user_profile");
String backgroundFromFirestore = (String) userProfile.get("activeBackground");
```

However, based on the `FirebaseDataManager.java` implementation, the `activeBackground` field is stored directly at the document root level, not nested under `user_profile`.

## Solution Implemented
Updated both methods in `OtherPlayerProfileActivity.java`:

### 1. `loadPlayerBackgroundFromDocument()` method
**Before:**
```java
// Get the user_profile data that contains activeBackground
java.util.Map<String, Object> userProfile = (java.util.Map<String, Object>) document.get("user_profile");
if (userProfile != null) {
    String backgroundFromFirestore = (String) userProfile.get("activeBackground");
    // ...
}
```

**After:**
```java
// Get the activeBackground field directly from document root (not from user_profile)
String backgroundFromFirestore = document.getString("activeBackground");
if (backgroundFromFirestore != null && !backgroundFromFirestore.trim().isEmpty()) {
    activeBackground = backgroundFromFirestore;
}
```

### 2. `loadPlayerBackgroundFromFirestore()` method
Applied the same fix to ensure consistency between both code paths.

## Technical Details
- **Data Structure**: `activeBackground` is stored as a direct field in the Firestore user document
- **Reference**: This matches the pattern used in `FirebaseDataManager.updateProfileCustomization()` where updates are applied directly:
  ```java
  updates.put("activeBackground", activeBackground);
  ```
- **Consistency**: The fix aligns with how `ProfileFragment.java` loads the background through `FirebaseDataManager.loadProfileCustomization()`

## Verification
1. The code now correctly reads `activeBackground` from the document root
2. No more fallback to nested `user_profile.activeBackground`
3. Maintains the same error handling and default fallback behavior
4. Both loading paths (from document and from separate Firestore call) are consistent

## Expected Result
When visiting another player's profile:
- The logcat should now show the actual background value (e.g., "nature", "ocean", etc.) instead of always "default"
- The profile background should visually match the visited user's selected background
- The background should change correctly when visiting different users with different background preferences

## Files Modified
- `app/src/main/java/com/example/glean/activity/OtherPlayerProfileActivity.java`
  - Fixed `loadPlayerBackgroundFromDocument()` method
  - Fixed `loadPlayerBackgroundFromFirestore()` method

## Testing Checklist
- [ ] Visit profile of user with "nature" background
- [ ] Visit profile of user with "ocean" background  
- [ ] Visit profile of user with "sunset" background
- [ ] Visit profile of user with "galaxy" background
- [ ] Visit profile of user with "default" background
- [ ] Check logcat shows correct background values
- [ ] Verify visual background changes match the user's selection
