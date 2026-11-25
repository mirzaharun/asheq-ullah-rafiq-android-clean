# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve Firestore data models used via reflection (.toObject / @PropertyName)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * { *; }
-keep class com.example.data.model.** { *; }

# Preserve FCM service entry point
-keep class com.example.service.PortalFirebaseMessagingService { *; }
