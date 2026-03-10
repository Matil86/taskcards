# Firestore Security Rules Deployment

## Overview
This document describes how to deploy the Firestore security rules to your Firebase project.

## Current Status
✅ Security rules file created: `firestore.rules`
⚠️ **IMPORTANT**: Rules must be deployed to Firebase Console to take effect!

## Security Rules Overview

The current rules implement authentication-based access control with ownership-restricted deletes:
- All operations require user authentication (`request.auth != null`)
- Read, create, and update on lists are allowed for any authenticated user (enables QR-code sharing)
- **Delete on a list is restricted to the list owner** — tracked via `/users/{uid}/lists/{listId}`
- Task subcollection writes are validated against data structure and business rules
- Enforces business rules (text length ≤ 500 chars, order bounds)

### Sharing Model

TaskCards uses "access by link" sharing (similar to Google Docs shared links). List IDs are cryptographically random Firestore-generated IDs (20 chars, base62) and are not guessable. Any authenticated user who obtains the ID via QR code or deep link may read and collaborate, but only the original owner may delete the list.

## Deployment Instructions

### Prerequisites
- Firebase CLI installed: `npm install -g firebase-tools`
- Authenticated with Firebase: `firebase login`
- Firebase project initialized: `firebase init firestore` (if not already done)

### Deploy Rules

```bash
# Deploy only Firestore security rules
firebase deploy --only firestore:rules

# Or deploy all Firebase resources
firebase deploy
```

### Verify Deployment

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `taskcards-dbcd0`
3. Navigate to **Firestore Database** → **Rules** tab
4. Verify the rules match the content of `firestore.rules`
5. Check that the rules are **Published** (not in draft mode)

## Testing Rules

### Test in Firebase Console

1. Go to Firestore Rules tab in Firebase Console
2. Click **Rules Playground**
3. Test scenarios:
   - **Authenticated read**: Should succeed
   - **Unauthenticated read**: Should fail
   - **Write with invalid data**: Should fail

### Test in App

```kotlin
// This should work (authenticated user)
val tasks = firestoreRepository.observeTasks("test-list-id").first()

// This should fail (no auth token)
// Firebase SDK will throw FirebaseFirestoreException
```

## Security Checklist

Before deploying to production:

- [ ] Rules deployed to Firebase Console
- [ ] Verified rules are active (not in draft)
- [ ] Tested authenticated access (should work)
- [ ] Tested unauthenticated access (should fail)
- [ ] Tested with invalid task data (should fail)
- [ ] Tested list delete by owner (should work)
- [ ] Tested list delete by non-owner (should fail)
- [ ] **TODO**: Add tests for security rules (using Firebase Emulator)

## Monitoring

After deployment, monitor Firestore usage in Firebase Console:
- **Usage** tab: Check for unexpected read/write patterns
- **Indexes** tab: Ensure required indexes are created
- **Rules** tab: Review any rule evaluation errors

## Emergency Rollback

If rules cause production issues:

```bash
# Rollback to previous version
firebase deploy --only firestore:rules --version <version-number>

# Or manually in Firebase Console:
# Go to Rules tab → Click version history → Select previous version → Publish
```

## References

- [Firestore Security Rules Documentation](https://firebase.google.com/docs/firestore/security/get-started)
- [Firebase Security Rules Reference](https://firebase.google.com/docs/rules/rules-language)
- [Firebase CLI Documentation](https://firebase.google.com/docs/cli)
