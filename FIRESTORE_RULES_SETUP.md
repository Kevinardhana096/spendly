# Firestore Security Rules untuk Spendly App

## Problem yang Kemungkinan Terjadi
Data signup tidak tersimpan di Firestore karena **Firestore Security Rules** yang terlalu ketat atau tidak dikonfigurasi dengan benar.

## Solusi: Konfigurasi Firestore Security Rules

### 1. Buka Firebase Console
1. Pergi ke [Firebase Console](https://console.firebase.google.com/)
2. Pilih project **spendly-c32f6**
3. Pilih **Firestore Database** dari menu kiri
4. Klik tab **Rules**

### 2. Rules untuk Development (Temporary)
Untuk testing sementara, gunakan rules ini yang mengizinkan authenticated users untuk read/write:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow authenticated users to read and write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Allow authenticated users to access their subcollections
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Temporary: Allow all authenticated users to write to test collection
    match /test/{document} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 3. Rules untuk Production (Lebih Aman)
Setelah testing berhasil, gunakan rules yang lebih aman ini:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own data
    match /users/{userId} {
      allow read, write: if request.auth != null 
        && request.auth.uid == userId
        && isValidUserData(resource.data);
    }
    
    // User subcollections (transactions, savings, budget)
    match /users/{userId}/transactions/{transactionId} {
      allow read, write: if request.auth != null 
        && request.auth.uid == userId;
    }
    
    match /users/{userId}/savings/{savingsId} {
      allow read, write: if request.auth != null 
        && request.auth.uid == userId;
    }
    
    match /users/{userId}/budget/{budgetDoc} {
      allow read, write: if request.auth != null 
        && request.auth.uid == userId;
    }
    
    // Helper function to validate user data
    function isValidUserData(data) {
      return data.keys().hasAll(['email', 'currentBalance']) 
        && data.email is string 
        && data.currentBalance is number;
    }
  }
}
```

### 4. Rules Paling Permisif (Hanya untuk Testing)
Jika masih ada masalah, gunakan rules ini SEMENTARA untuk testing:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**⚠️ PERINGATAN: Rules terakhir ini tidak aman untuk production!**

### 5. Cara Apply Rules
1. Copy salah satu rules di atas
2. Paste ke editor rules di Firebase Console
3. Klik **Publish** untuk menerapkan rules
4. Test signup lagi di aplikasi

### 6. Verifikasi Rules Bekerja
Setelah apply rules, coba:
1. Jalankan aplikasi
2. Coba signup dengan akun baru
3. Check logs di Android Studio untuk melihat pesan dari `FirestoreTestUtils`
4. Check Firebase Console > Firestore > Data untuk melihat apakah document user tersimpan

### 7. Debug dengan Logs
Aplikasi sekarang memiliki detailed logging. Check logcat untuk pesan seperti:
- `✅ Firestore write test SUCCESSFUL`
- `❌ PERMISSION_DENIED: Check Firestore Security Rules`
- `✅ User data saved successfully to Firestore`

## Troubleshooting Tambahan

### Jika Masih Bermasalah:
1. **Check Network Connection**: Pastikan device/emulator terkoneksi internet
2. **Check Firebase Project**: Pastikan `google-services.json` sesuai dengan project yang benar
3. **Check Package Name**: Pastikan package name di `google-services.json` cocok dengan app (`com.example.spendly`)
4. **Restart App**: Setelah ganti rules, restart aplikasi sepenuhnya

### Error Messages yang Mungkin Muncul:
- `PERMISSION_DENIED` → Rules terlalu ketat
- `NOT_FOUND` → Project atau collection tidak ditemukan  
- `UNAUTHENTICATED` → User belum login dengan benar
- `NETWORK_ERROR` → Masalah koneksi internet
