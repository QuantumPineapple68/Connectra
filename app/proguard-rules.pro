# Keep model classes
-keep class com.nachiket.connectra.model.** { *; }

# Keep Firebase related classes
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep Firebase Database classes
-keep class com.google.firebase.database.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.auth.** { *; }

# Keep your model classes
-keep class com.nachiket.connectra.model.ConnectionRequest { *; }
-keep class com.nachiket.connectra.model.ChatTexts { *; }
-keep class com.nachiket.connectra.model.User { *; }
-keep class com.nachiket.connectra.model.NewUser { *; }

# Keep serialization methods
-keepclassmembers class com.nachiket.connectra.model.** {
    void set*(***);
    *** get*();
}

-keepattributes SourceFile,LineNumberTable
-keep class androidx.appcompat.widget.** { *; }