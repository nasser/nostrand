# AOT
echo "Nostrand Release Build"
echo "AOTing Clojure Source..."
mono Nostrand.exe nostrand.bootstrap/full-aot
echo nostrand.*.dll
echo "Merging assemblies is breaking things... skipping for now..."
# mono ../../packages/ILRepack.2.0.12/tools/ILRepack.exe /out:nos.exe Nostrand.exe Clojure.dll nostrand.*.dll
echo "AOTing Assemblies..."
mono --arch=64 --aot *.dll
mono --arch=64 --aot *.exe
echo "Done!"

# unpatch AssemblyInfo
cd ../..
mv Properties/AssemblyInfo.cs-unpatched Properties/AssemblyInfo.cs