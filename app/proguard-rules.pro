# Add project specific ProGuard rules here.
# Phase 1: no minification enabled by default; rules kept minimal and ready for Phase 2.

-keepattributes *Annotation*
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
