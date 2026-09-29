# IronAPP ProGuard rules.
-keep class com.ironpanel.app.data.** { *; }
-keep class com.wireguard.** { *; }
-dontwarn com.wireguard.**
-keepattributes Signature, InnerClasses, EnclosingMethod
