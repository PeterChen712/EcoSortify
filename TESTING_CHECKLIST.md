# Testing Checklist for Profile Customization

## Pre-Testing Setup
- [ ] Ensure Firebase is configured and connected
- [ ] User is logged in to Firebase
- [ ] User has some points available for testing purchases

## Background Customization Tests

### 1. Background Selection
- [ ] Open Customize Profile → Backgrounds tab
- [ ] Verify current background is displayed
- [ ] Select a different owned background
- [ ] Press Save
- [ ] **Expected:** Background changes in profile view
- [ ] **Expected:** Background persists after app restart

### 2. Background Purchase
- [ ] Navigate to locked/premium background
- [ ] Note current point balance
- [ ] Attempt to purchase background
- [ ] **Expected:** Points are deducted correctly
- [ ] **Expected:** Background becomes available for selection
- [ ] **Expected:** Purchase is saved to Firebase

### 3. Insufficient Points Test
- [ ] Find expensive background that costs more than current points
- [ ] Attempt to purchase
- [ ] **Expected:** Error message appears
- [ ] **Expected:** No points are deducted
- [ ] **Expected:** Background remains locked

## Badge Customization Tests

### 1. Badge Selection
- [ ] Open Customize Profile → Badges tab
- [ ] Verify current selected badges (should show count)
- [ ] Select/deselect badges (max 3)
- [ ] Press Save
- [ ] **Expected:** Selected badges appear in profile view
- [ ] **Expected:** Badge selection persists after app restart

### 2. Badge Limit Test
- [ ] Try to select more than 3 badges
- [ ] **Expected:** Error message about maximum limit
- [ ] **Expected:** Only 3 badges can be selected

### 3. Minimum Badge Test
- [ ] Try to deselect all badges
- [ ] **Expected:** Error message about minimum 1 badge required
- [ ] **Expected:** At least 1 badge remains selected

## Firebase Integration Tests

### 1. Data Persistence
- [ ] Make changes to profile customization
- [ ] Force close app completely
- [ ] Reopen app
- [ ] **Expected:** All changes are preserved
- [ ] **Expected:** Profile displays correctly

### 2. Network Offline Test
- [ ] Disconnect from internet
- [ ] Make profile changes
- [ ] **Expected:** Changes saved locally
- [ ] Reconnect to internet
- [ ] **Expected:** Changes sync to Firebase

### 3. Point Synchronization
- [ ] Purchase background on one device
- [ ] Check points on another device (if available)
- [ ] **Expected:** Point balance is consistent across devices

## Backward Compatibility Tests

### 1. Legacy Data Migration
- [ ] If possible, test with user who has old SharedPreferences data
- [ ] **Expected:** Old data is migrated to Firebase
- [ ] **Expected:** No data loss occurs

### 2. Fallback Functionality
- [ ] Temporarily disable Firebase (if possible)
- [ ] **Expected:** App falls back to SharedPreferences
- [ ] **Expected:** Basic functionality still works

## UI/UX Tests

### 1. Profile Display
- [ ] Verify background displays correctly in profile
- [ ] Verify badges display correctly (up to 3)
- [ ] Check that UI is responsive and smooth

### 2. Customize Profile Activity
- [ ] Verify tabs work correctly
- [ ] Verify Save/Back buttons function properly
- [ ] Check that unsaved changes dialog appears when needed

### 3. Error Handling
- [ ] Test various error scenarios
- [ ] **Expected:** User-friendly error messages
- [ ] **Expected:** App doesn't crash on errors

## Performance Tests

### 1. Loading Speed
- [ ] Time how long it takes to load profile customization
- [ ] **Expected:** Under 2 seconds for normal network
- [ ] **Expected:** Smooth user experience

### 2. Memory Usage
- [ ] Monitor app memory during customization
- [ ] **Expected:** No significant memory leaks
- [ ] **Expected:** Reasonable memory usage

## Edge Cases

### 1. New User
- [ ] Test with brand new user account
- [ ] **Expected:** Default badge and background assigned
- [ ] **Expected:** Profile initialization works correctly

### 2. User with No Points
- [ ] Test with user who has 0 points
- [ ] **Expected:** Can still customize with owned items
- [ ] **Expected:** Cannot purchase new items

### 3. Corrupted Data
- [ ] Test with invalid/corrupted profile data
- [ ] **Expected:** App handles gracefully
- [ ] **Expected:** Falls back to defaults if needed

## Success Criteria

### Critical Requirements
✅ **Point Deduction:** Points are correctly deducted when purchasing backgrounds
✅ **Data Persistence:** Profile customization persists across app sessions  
✅ **Firebase Storage:** Data is saved to and loaded from Firebase
✅ **UI Updates:** Profile display reflects current customization settings

### Additional Requirements
✅ **Badge Display:** Up to 3 selected badges appear in profile
✅ **Background Display:** Active background is shown in profile
✅ **Error Handling:** Graceful handling of network errors and edge cases
✅ **Backward Compatibility:** Existing user data is preserved and migrated

## Known Issues & Limitations

### Current Limitations
- Background previews are static (not animated)
- Limited selection of backgrounds and badges
- No background/badge sharing between users

### Future Improvements
- Add more background options
- Implement background preview animations
- Add premium badge purchase system
- Enable social sharing of customized profiles

## Testing Environment
- **Platform:** Android
- **Firebase:** Firestore integration
- **Network:** Test both online and offline scenarios
- **Devices:** Test on different screen sizes if possible
