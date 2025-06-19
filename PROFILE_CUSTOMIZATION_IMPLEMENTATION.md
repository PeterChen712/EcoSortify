# Profile Customization Implementation

## Overview
This implementation fixes the bug where profile customization (backgrounds/skins and badges) were not being saved properly and adds Firebase integration for persistent data storage.

## Key Changes Made

### 1. Enhanced UserProfile Model (FirebaseDataManager.java)
- Added new fields for profile customization:
  - `List<String> selectedBadges` - Array of selected badges (max 3)
  - `List<String> ownedBackgrounds` - Array of owned backgrounds
  - `String activeBackground` - Currently active background ID
- Removed unused `totalPoints` and `totalKm` fields that were duplicated from user_stats

### 2. Firebase Integration Methods
Added new methods in `FirebaseDataManager.java`:

#### Point Management
- `updateUserPointsAfterPurchase(int pointsToDeduct, ProfileCustomizationCallback callback)`
  - Deducts points from user's Firebase stats
  - Updates local database as well
  - Validates sufficient points before purchase

#### Profile Customization
- `updateProfileCustomization(List<String> selectedBadges, List<String> ownedBackgrounds, String activeBackground, ProfileCustomizationCallback callback)`
  - Updates all profile customization data in Firebase
  - Saves selected badges, owned backgrounds, and active background

#### Background Purchase
- `purchaseBackground(String backgroundId, int price, ProfileCustomizationCallback callback)`
  - Handles complete background purchase flow
  - Deducts points and adds background to owned list
  - Atomic operation - either succeeds completely or fails

#### Data Loading
- `loadProfileCustomization(ProfileDataCallback callback)`
  - Loads all profile customization data from Firebase
  - Provides default values if data doesn't exist
- `initializeDefaultProfileCustomization(ProfileCustomizationCallback callback)`
  - Sets up default profile data for new users

### 3. SkinSelectionFragment Updates
**Key Changes:**
- Added Firebase integration with fallback to SharedPreferences
- `loadCurrentSkin()` now loads from Firebase first, then falls back to local storage
- `generateAvailableSkins()` uses Firebase data for owned backgrounds
- `onSkinPurchase()` uses Firebase to handle purchases and point deduction
- `saveSelection()` saves to Firebase with SharedPreferences fallback

**Data Flow:**
1. Load profile data from Firebase
2. Display available skins based on owned backgrounds
3. Allow purchase with real-time point validation
4. Save selection to Firebase and local storage

### 4. BadgeSelectionFragment Updates
**Key Changes:**
- Added Firebase integration with fallback to SharedPreferences
- `loadCurrentBadge()` loads selected badges from Firebase
- Badge selection (max 3) is saved to Firebase
- Backward compatibility with legacy single badge system

**Features:**
- Max 3 badges can be selected and displayed
- At least 1 badge must always be selected
- Loads from Firebase with SharedPreferences fallback
- Saves to both Firebase and local storage

### 5. ProfileFragment Updates
**Background Display:**
- `updateProfileSkin()` loads active background from Firebase
- Falls back to SharedPreferences if Firebase fails
- Applies correct background resource to profile view

**Badge Display:**
- `setupBadges()` loads selected badges from Firebase
- Creates badge objects with proper icons and metadata
- Displays up to 3 selected badges in profile

## Technical Implementation Details

### Error Handling
- All Firebase operations have error callbacks
- Fallback to SharedPreferences for backward compatibility
- Local database sync for offline support
- Validation for insufficient points before purchases

### Data Persistence
- **Primary Storage:** Firebase Firestore
- **Fallback Storage:** SharedPreferences (for backward compatibility)
- **Local Cache:** SQLite database (for offline access)

### Real-time Updates
- Profile customization data is loaded from Firebase on app start
- Changes are immediately saved to Firebase
- Local UI updates happen immediately for responsive experience
- Background sync ensures data consistency

## Database Schema

### Firebase Collections

#### users/{userId}
```json
{
  "selectedBadges": ["starter", "green_helper", "eco_warrior"],
  "ownedBackgrounds": ["default", "nature", "ocean"],
  "activeBackground": "nature",
  "lastUpdated": 1671234567890,
  // ... other user profile fields
}
```

#### user_stats/{userId}
```json
{
  "currentPoints": 250,
  "totalPoints": 500,
  "totalKm": 12.5,
  "totalTrashCollected": 25,
  "currentLevel": 3,
  "lastUpdated": 1671234567890
}
```

## Usage Instructions

### For Users
1. **Purchasing Backgrounds:**
   - Navigate to Customize Profile → Backgrounds tab
   - Select a locked background to see purchase option
   - Confirm purchase (points will be deducted)
   - Background is immediately available for selection

2. **Selecting Backgrounds:**
   - Choose from owned backgrounds
   - Press Save to apply changes
   - Background persists across app sessions

3. **Selecting Badges:**
   - Navigate to Customize Profile → Badges tab
   - Select up to 3 badges to display
   - Changes are saved automatically
   - Badges appear in profile view

### For Developers
1. **Adding New Backgrounds:**
   - Add drawable resource to `res/drawable/`
   - Update `generateAvailableSkins()` method
   - Add case in `getSkinResource()` method

2. **Adding New Badges:**
   - Add drawable resource for badge icon
   - Update `generateUserBadges()` method
   - Add case in `createBadgeFromId()` method

## Migration & Backward Compatibility

### Existing Users
- Old SharedPreferences data is automatically detected
- Data is gradually migrated to Firebase on first use
- No data loss during transition
- Fallback mechanisms ensure app continues working

### New Users
- Default profile customization is automatically initialized
- Starter badge and default background are provided
- Profile data is created in Firebase from first use

## Testing Scenarios

### Successful Purchase Flow
1. User has sufficient points
2. Background is purchased successfully
3. Points are deducted from Firebase
4. Background is added to owned list
5. UI updates immediately
6. Data persists across app restart

### Insufficient Points
1. User attempts to purchase expensive background
2. Validation prevents purchase
3. Error message is displayed
4. No points are deducted
5. Background remains locked

### Offline/Network Error
1. Network connectivity lost
2. Changes saved to SharedPreferences
3. Firebase sync happens when connection restored
4. No data loss occurs

## Future Enhancements

### Planned Features
1. **Premium Badge System:** Purchasable badges with special effects
2. **Seasonal Backgrounds:** Time-limited backgrounds for events
3. **Achievement System:** Unlock backgrounds through specific actions
4. **Social Features:** Share customized profiles with friends
5. **Profile Themes:** Complete visual theme packages

### Technical Improvements
1. **Caching Strategy:** Implement more sophisticated caching
2. **Sync Optimization:** Batch updates for better performance
3. **Image Loading:** Lazy loading for background previews
4. **Animation Effects:** Smooth transitions between backgrounds

## Troubleshooting

### Common Issues
1. **Profile not saving:** Check Firebase authentication status
2. **Background not displaying:** Verify drawable resources exist
3. **Points not deducting:** Check network connectivity and Firebase rules
4. **Badges not showing:** Ensure ProfileBadgeAdapter is properly initialized

### Debug Information
- Enable debug logging with `TAG = "ProfileCustomization"`
- Check Firebase console for data structure
- Monitor SharedPreferences for fallback data
- Verify local database sync status
