# Add project specific ProGuard rules here.

# Keep Room entities
-keep class com.chastity.diary.data.local.entity.** { *; }

# Keep Room DAOs
-keep interface com.chastity.diary.data.local.dao.** { *; }

# Keep Gson serialized classes
-keep class com.chastity.diary.domain.model.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }

# Tink / security-crypto: suppress missing annotation classes (not needed at runtime)
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
