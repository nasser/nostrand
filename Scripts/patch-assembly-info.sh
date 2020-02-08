# http://stackoverflow.com/questions/2657935/checking-for-a-dirty-index-or-untracked-files-with-git
# cd ../../
DIRTY=$([[ $(git diff --shortstat 2> /dev/null | tail -n1) != "" ]] && echo "*")
INFO=$(echo "(`git rev-parse --abbrev-ref HEAD`\/`git describe --always`$DIRTY `date`)")
sed -i "s/AssemblyInformationalVersion.*/AssemblyInformationalVersion(\"$INFO\")]/g" Properties/AssemblyInfo.cs
