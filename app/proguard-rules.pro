# IronAPP ProGuard rules.
-keep class com.ironpanel.app.data.** { *; }
-keep class com.ironpanel.libbox.** { *; }
-keep class go.Seq { *; }
-dontwarn com.ironpanel.libbox.**
-dontwarn go.**
-keepattributes Signature, InnerClasses, EnclosingMethod
