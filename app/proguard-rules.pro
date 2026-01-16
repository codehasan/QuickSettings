-keepattributes SourceFile,LineNumberTable,*Annotation*

# 1. Keep the annotation itself so it isn't removed
-keep @interface io.github.codehasan.quicksettings.annotations.MinSdk

# 2. Ensure annotations are actually written to the DEX file
-keepattributes RuntimeVisibleAnnotations