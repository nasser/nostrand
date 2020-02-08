# unpatch AssemblyInfo
sed -i "s/AssemblyInformationalVersion.*/AssemblyInformationalVersion(\"\")]/g" Properties/AssemblyInfo.cs