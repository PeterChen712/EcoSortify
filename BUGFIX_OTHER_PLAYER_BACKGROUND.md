# Bug Fix: Background Profile Other Player Tidak Update

## Deskripsi Masalah
Saat mengunjungi profile user lain (other player profile/activity) dari akun berbeda, background profil yang tampil tidak sesuai dengan background terakhir yang sudah diganti oleh user tersebut. Background yang ditampilkan tetap menggunakan background default, padahal user tersebut sudah mengganti background dan tersimpan di Firestore.

## Akar Masalah
`OtherPlayerProfileActivity` tidak memuat data `activeBackground` dari field `user_profile.activeBackground` di Firestore milik user yang dikunjungi. Activity ini hanya menampilkan background default tanpa mengecek preferensi background aktual dari user yang dikunjungi.

## Solusi yang Diterapkan

### 1. Menambahkan Fungsi `loadPlayerBackgroundFromFirestore()`
Fungsi ini mengambil data background aktif dari Firestore untuk user yang dikunjungi:
- Melakukan query ke collection `users` dengan document ID sesuai player yang dikunjungi
- Mengakses field `user_profile.activeBackground` 
- Mengaplikasikan background yang sesuai ke layout profile
- Menggunakan fallback ke background default jika data tidak tersedia

### 2. Menambahkan Fungsi `loadPlayerBackgroundFromDocument()`
Fungsi helper untuk memuat background dari DocumentSnapshot yang sudah ada:
- Memparse data `user_profile.activeBackground` dari document yang sudah di-fetch
- Lebih efisien karena tidak perlu query tambahan jika sudah ada DocumentSnapshot

### 3. Menambahkan Fungsi `getSkinResource()`
Fungsi utility untuk mapping skin ID ke drawable resource:
- Mengkonversi string skin ID ("nature", "ocean", "sunset", "galaxy") ke resource drawable yang sesuai
- Menggunakan logic yang sama dengan `ProfileFragment` untuk konsistensi
- Fallback ke `profile_skin_default` untuk skin ID yang tidak dikenali

### 4. Menambahkan Fungsi `applyDefaultBackground()`
Fungsi fallback untuk mengaplikasikan background default:
- Dipanggil ketika terjadi error atau data tidak tersedia
- Memastikan UI tetap konsisten meskipun gagal memuat data background

### 5. Update Logika `displayPlayerData()` dan `handleFirebaseData()`
- `handleFirebaseData()`: Memuat background langsung dari DocumentSnapshot yang di-fetch
- `displayPlayerData()`: Memuat background dari Firestore terpisah hanya jika data berasal dari ranking cache
- Mencegah double-loading background untuk optimisasi performa

## Perubahan Kode

### File: `OtherPlayerProfileActivity.java`

#### Fungsi Baru yang Ditambahkan:
1. `loadPlayerBackgroundFromFirestore(String userId)` - Memuat background dari Firestore
2. `loadPlayerBackgroundFromDocument(DocumentSnapshot document)` - Memuat background dari document yang sudah ada
3. `getSkinResource(String skinId)` - Mapping skin ID ke drawable resource
4. `applyDefaultBackground()` - Aplikasi background default sebagai fallback

#### Fungsi yang Diperbarui:
1. `handleFirebaseData()` - Ditambahkan pemanggilan `loadPlayerBackgroundFromDocument()`
2. `displayPlayerData()` - Ditambahkan logika kondisional untuk loading background

## Cara Kerja Perbaikan

### Skenario 1: Data dari Ranking Cache
1. User mengklik profile player lain dari ranking
2. `displayPlayerData()` dipanggil dengan data `RankingUser`
3. Karena `rankingUser != null`, system memanggil `loadPlayerBackgroundFromFirestore()`
4. Dilakukan fetch fresh data dari Firestore untuk mengambil `activeBackground`
5. Background diterapkan sesuai preferensi user yang dikunjungi

### Skenario 2: Data dari Direct Firebase Query
1. User mengakses profile player dengan player ID
2. `loadFromFirebase()` dipanggil untuk fetch data lengkap
3. `handleFirebaseData()` memproses DocumentSnapshot dan memanggil `loadPlayerBackgroundFromDocument()`
4. Background langsung dimuat dari document yang sama tanpa query tambahan
5. `displayPlayerData()` tidak perlu load background lagi karena sudah dimuat

## Hasil Perbaikan
✅ **Background profile other player sekarang update secara real-time**
✅ **Mengambil data langsung dari field `activeBackground` di Firestore**
✅ **Tidak menggunakan cache atau data lama**  
✅ **Menampilkan background sesuai preferensi user yang dikunjungi, bukan user yang sedang login**
✅ **Fallback mechanism memastikan UI tetap konsisten meskipun ada error**
✅ **Optimisasi performa dengan mencegah double-loading**

## Testing
Untuk menguji perbaikan ini:
1. Login dengan user A
2. Ganti background profile user A ke background non-default (misal: "nature")
3. Login dengan user B
4. Dari user B, kunjungi profile user A melalui ranking
5. Verifikasi background yang tampil sesuai dengan background yang dipilih user A ("nature"), bukan background default

## Catatan Teknis
- Perbaikan ini memastikan konsistensi dengan cara `ProfileFragment` memuat background untuk user sendiri
- Menggunakan structure data yang sama (`user_profile.activeBackground`) dengan sistem profile customization yang sudah ada
- Error handling yang robust untuk memastikan aplikasi tidak crash jika data background tidak tersedia
- Kompatibel dengan sistem background yang sudah ada tanpa memerlukan perubahan skema database
