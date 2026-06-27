# SwiftLite Keyboard ProGuard rules

# Keep Room entities
-keep class com.swiftlite.keyboard.clipboard.ClipboardItem { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# Keep IME service
-keep class com.swiftlite.keyboard.ime.SwiftLiteIME { *; }
-keep class com.swiftlite.keyboard.SetupActivity { *; }
-keep class com.swiftlite.keyboard.clipboard.ClipboardShareActivity { *; }

# Keep spell checker listener
-keep class com.swiftlite.keyboard.suggestions.SuggestionEngine { *; }

# AndroidX
-dontwarn androidx.**

# Material components
-keep class com.google.android.material.** { *; }
