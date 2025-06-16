# Firebase Setup untuk EcoSortify

## Setup Firebase Authentication & Firestore

### 1. Buat Project Firebase

1. Kunjungi [Firebase Console](https://console.firebase.google.com/)
2. Klik "Add Project" atau "Create a project"
3. Masukkan nama project (contoh: "ecosortify-plogging")
4. Ikuti langkah-langkah setup

### 2. Setup Authentication

1. Di Firebase Console, pilih **Authentication** dari menu kiri
2. Klik tab **Sign-in method**
3. Enable provider yang diinginkan:
   - **Email/Password**: Klik dan enable
   - **Google**: Klik, enable, dan masukkan support email

### 3. Setup Firestore Database

1. Di Firebase Console, pilih **Firestore Database**
2. Klik **Create database**
3. Pilih **Start in test mode** (untuk development)
4. Pilih lokasi server (pilih yang terdekat dengan Indonesia)

### 4. Add Android App

1. Di Firebase Console, klik **Project settings** (gear icon)
2. Klik **Add app** dan pilih **Android**
3. Masukkan package name: `com.example.glean`
4. Download file `google-services.json`
5. **PENTING**: Copy file tersebut ke folder `app/` (sejajar dengan `build.gradle`)

### 5. Update Firebase Auth Manager

Setelah setup Google Sign-In, update `FirebaseAuthManager.java`:

```java
// Di constructor FirebaseAuthManager, ganti "YOUR_WEB_CLIENT_ID" dengan Web Client ID dari Firebase
GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken("PASTE_YOUR_WEB_CLIENT_ID_HERE") // Ambil dari Firebase Console
    .requestEmail()
    .build();
```

**Cara mendapatkan Web Client ID:**
1. Di Firebase Console, pergi ke **Project settings**
2. Tab **General**, scroll ke bagian **Your apps**
3. Klik app Android Anda
4. Di bagian **Google Services**, copy **Web client ID**

### 6. Test Firebase Connection

Setelah setup:

1. Build dan jalankan aplikasi
2. Coba register dengan email/password
3. Coba login dengan Google
4. Periksa di Firebase Console:
   - **Authentication** tab untuk melihat user yang terdaftar
   - **Firestore** untuk melihat data user yang tersimpan

### 7. Firestore Security Rules (Opsional untuk Development)

Untuk development, Anda bisa menggunakan rules berikut di Firestore:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    match /plogging_sessions/{sessionId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
  }
}
```

### 8. Production Setup

Untuk production:

1. **Firestore Rules**: Update dengan rules yang lebih ketat
2. **Authentication**: Set domain authorization untuk web
3. **API Keys**: Restrict API keys di Google Cloud Console
4. **Monitoring**: Enable Firebase Analytics dan Crashlytics

### Troubleshooting

**Error "Default FirebaseApp is not initialized":**
- Pastikan `google-services.json` ada di folder `app/`
- Clean dan rebuild project

**Error Google Sign-In:**
- Pastikan Web Client ID sudah dimasukkan dengan benar
- Pastikan SHA-1 fingerprint sudah ditambahkan di Firebase Console

**Error Permission Denied di Firestore:**
- Periksa Firestore Security Rules
- Pastikan user sudah authenticated

### File Structure yang Benar

```
app/
├── google-services.json          ← File dari Firebase Console
├── build.gradle                  ← Updated dengan Firebase dependencies
└── src/main/java/com/example/glean/
    └── auth/
        ├── FirebaseAuthManager.java    ← Authentication logic
        └── AuthGuard.java             ← Protection untuk plogging features
```
