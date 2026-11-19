# Protobuf
-shrinkunusedprotofields

# Commons-compress
-dontwarn com.github.luben.zstd.**

# KitsuneSU FolkPatch JNI bridge
-keep class com.sukisu.nova.folkpatch.ApNatives { *; }
-keep class com.sukisu.nova.folkpatch.ApNatives$* { *; }
-keep class com.sukisu.nova.folkpatch.ApMagicaPreload { *; }
