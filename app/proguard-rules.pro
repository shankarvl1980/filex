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

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn javax.el.BeanELResolver
-dontwarn javax.el.ELContext
-dontwarn javax.el.ELResolver
-dontwarn javax.el.ExpressionFactory
-dontwarn javax.el.FunctionMapper
-dontwarn javax.el.ValueExpression
-dontwarn javax.el.VariableMapper
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid


# Keep Gson internal stuff (optional; safe)
-keep class com.google.gson.** { *; }

# ---- Keep ONLY DTO fields Gson reads via reflection ----

# GoogleDriveFileModel DTOs
-keepclassmembers class svl.kadatha.filex.filemodel.GoogleDriveFileModel$GoogleDriveFileMetadata {
    <fields>;
}
-keepclassmembers class svl.kadatha.filex.filemodel.GoogleDriveFileModel$DriveFilesListResponse {
    <fields>;
}

# SubFileCountUtil DTOs
-keepclassmembers class svl.kadatha.filex.SubFileCountUtil$YandexTotalResponse {
    <fields>;
}
-keepclassmembers class svl.kadatha.filex.SubFileCountUtil$YandexEmbedded {
    <fields>;
}

# FileCountSize DTOs
-keepclassmembers class svl.kadatha.filex.FileCountSize$DriveFileMeta {
    <fields>;
}
-keepclassmembers class svl.kadatha.filex.FileCountSize$DriveListResponse {
    <fields>;
}

# SMBJ core
-keep class com.hierynomus.smbj.** { *; }
-keep class com.hierynomus.msfscc.** { *; }
-keep class com.hierynomus.mssmb2.** { *; }
-dontwarn com.hierynomus.**

# SMBJ uses MBassador (reflection-heavy)
-keep class net.engio.mbassy.** { *; }
-keepclassmembers class net.engio.mbassy.** { *; }
-dontwarn net.engio.mbassy.**

# Reflection metadata (safe + helps multiple libs)
-keepattributes *Annotation*,Signature,EnclosingMethod,InnerClasses


# --- JSch (SFTP) uses reflection to load crypto / jce classes ---
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# If you use the fork com.github.mwiede:jsch, keep its packages too (safe)
-keep class com.github.mwiede.jsch.** { *; }
-dontwarn com.github.mwiede.jsch.**

