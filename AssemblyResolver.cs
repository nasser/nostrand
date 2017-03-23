using System;
using System.Reflection;
using clojure.lang;

namespace Nostrand
{
	public class AssemblyResolver
	{
		static Var resolveAssemblyLoadVar;

		static AssemblyResolver()
		{
			resolveAssemblyLoadVar = RT.var("nostrand.core", "resolve-assembly-load");
		}

		public static Assembly Resolve(object sender, ResolveEventArgs args)
		{
			return (Assembly)resolveAssemblyLoadVar.invoke(args.Name);
		}
	}
}
