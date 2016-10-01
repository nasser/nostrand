cd $1
mono --arch=64 --aot Nostrand.exe
mono --arch=64 --aot Clojure.dll
mkbundle --deps -c -oo temp.o -o temp.c Nostrand.exe Clojure.dll
gcc -L/Library/Frameworks/Mono.framework/Versions/4.6.0/lib/  -I/Library/Frameworks/Mono.framework/Versions/4.6.0/include/mono-2.0/ -lmonosgen-2.0 -isysroot /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.11.sdk/ -framework Foundation -arch x86_64 -o nos temp.c temp.o
mkdir -p bundle
cp nos Clojure* bundle/
rm temp.*