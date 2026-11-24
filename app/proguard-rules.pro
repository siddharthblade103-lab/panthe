# Blade Release Hardening & Firestore Keep Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Firestore data models annotated with @Keep
-keep @androidx.annotation.Keep class * { *; }
-keep class com.example.data.model.** { *; }

# Strip verbose/debug logs in release builds to prevent runtime data leaks
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
