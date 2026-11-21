-repackageclasses 'x'
-allowaccessmodification
-optimizationpasses 3

# Keep AndroidManifest entry points
-keep public class com.my.apk.SmartEarningApp {
    *;
}
-keep public class com.my.apk.WorkSelectorActivity {
    *;
}
-keep public class com.my.apk.MainActivity {
    *;
}
-keep public class com.my.apk.MetaWorkActivity {
    *;
}
-keep public class com.my.apk.MetaToIgActivity {
    *;
}
-keep public class com.my.apk.BgOverlayService {
    *;
}

# Preserve WebView, JavascriptInterface & AndroidX callbacks
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepclassmembers class * extends android.webkit.WebViewClient {
    *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    *;
}

# OkHttp / Okio rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Strip verbose/debug logs in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}