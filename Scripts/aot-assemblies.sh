# env
OUT_DIR=$1

cd $OUT_DIR
# AOT
echo "AOTing Clojure Source..."
mono Nostrand.exe nostrand.bootstrap/full-aot
echo nostrand.*.dll
echo "AOTing Assemblies..."
mono --aot *.dll
mono --aot *.exe