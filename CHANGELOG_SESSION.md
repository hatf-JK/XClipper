# Changelog (Session: Firestore Migration)

This file documents all changes made during the Firestore migration session.

## Summary

This update migrates the clipboard syncing mechanism from Firebase Realtime Database to **Cloud Firestore**. It also upgrades the Windows application to .NET Framework 4.7.2 and implements a new AES-256 encryption standard due to the removal of `XClipper.Protect`.

## Modified Files

### Android (`XClipper.Android`)

- **`buildSrc/src/main/java/LibraryDependency.kt`**:
    - Removed `firebase-database`.
    - Added `firebase-firestore`.

- **`modules/core/build.gradle.kts`**:
    - Updated dependencies to use `firebase-firestore`.

- **`app/build.gradle.kts`**:
    - Updated dependencies to use `firebase-firestore`.

- **`app/src/main/kotlin/com/kpstv/xclipper/data/provider/FirebaseProviderImpl.kt`**:
    - Fully refactored to use `FirebaseFirestore` instead of `FirebaseDatabase`.
    - Implemented `initialize`, `uploadData`, and `observeDataChange` using Firestore SDK methods.
    - Updated data path to `users/{uid}/Clips`.

- **`modules/core/src/main/java/com/kpstv/xclipper/extensions/ClipExtensions.kt`**:
    - Updated `encrypt()` and `decrypt()` extension methods.
    - Implemented `CryptoHelper` using standard AES-256 (PBKDF2 with HMAC-SHA1) to replace `XClipper.Protect` functionality.
    - Added `encryptBase64()` and `decryptBase64()` logic compatible with C# implementation.

### Windows (`XClipper.App`)

- **`XClipper.App.csproj`**:
    - Changed `TargetFrameworkVersion` to `v4.7.2`.
    - Removed `FireSharp` and `Firebase.Storage` references.
    - Added `Google.Cloud.Firestore` package reference.
    - Added `Core.cs` to compile items.

- **`Data/helpers/FirebaseSingleton.cs`**:
    - Deprecated and stubbed out to resolve build errors with removed dependencies.

- **`Data/helpers/FirebaseSingletonV2.cs`**:
    - **New Implementation**: Rewritten to use `Google.Cloud.Firestore`.
    - Implemented `Initialize` using service account credentials.
    - Implemented `AddClip`, `UpdateData`, `DeleteClip` using Firestore document updates.
    - Implemented real-time listening with `Listen` method.
    - Handles data mapping between Firestore documents and `User` model.

- **`Data/helpers/FirebaseHelper.cs`**:
    - Refactored to act as a wrapper for `FirebaseSingletonV2`.
    - Removed `FireSharp`/`RestSharp` dependent code.
    - Updated `EncryptBase64` and `DecryptBase64` to use the new `Core` class.

- **`Data/helpers/Core.cs`** (New File):
    - Implemented `EncryptBase64` and `DecryptBase64` using `Aes` and `Rfc2898DeriveBytes`.
    - Provides consistent encryption logic across platforms.

## Dependency Changes

- **Android**:
    - `com.google.firebase:firebase-database-ktx` -> `com.google.firebase:firebase-firestore-ktx`
- **Windows**:
    - `FireSharp` (Removed)
    - `Firebase.Storage` (Removed)
    - `Google.Cloud.Firestore` (Added)

## Notes

> [!WARNING]
> **Encryption Change**: The encryption logic has changed. Old encrypted data stored in the cloud or locally will not be readable. Users must re-sync their clipboard history.
