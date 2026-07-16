# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 高德地图 ProGuard 规则
-dontwarn com.amap.api.**
-keep class com.amap.api.** {*;}
-keep interface com.amap.api.** {*;}
-keep class com.amap.apis.** {*;}
-keep interface com.amap.apis.** {*;}
-keep class com.loc.** {*;}
-keep interface com.loc.** {*;}

# 处理重复类
-dontwarn com.amap.apis.utils.core.api.**
-keep class com.amap.apis.utils.core.api.AMapUtilCoreApi {*;}
-keep class com.amap.apis.utils.core.api.NetProxy {*;}