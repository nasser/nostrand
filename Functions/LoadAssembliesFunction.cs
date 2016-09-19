using System;
using System.Collections;
using clojure.lang;

namespace Nostrand
{
	public class LoadAssembliesFunction : AFn
	{
		public override object invoke(object options)
		{
			var verbose = ((IPersistentMap)options).valAt(Keyword.intern("verbose"));
			var assemblies = ((IPersistentMap)options).valAt(Keyword.intern("assemblies"));
			if (assemblies == null)
				return options;

			if (assemblies is string ||
				assemblies is Symbol)
			{
				System.Reflection.Assembly.LoadFrom(assemblies.ToString());
				if (verbose != null && (bool)verbose == true)
				{
					Terminal.Message("Assembly", assemblies, ConsoleColor.DarkBlue);
				}
			}

			else if (assemblies is IEnumerable)
			{
				foreach (var assembly in (IEnumerable)assemblies)
				{
					System.Reflection.Assembly.LoadFrom(assembly.ToString());
					if (verbose != null && (bool)verbose == true)
					{
						Terminal.Message("Assembly", assembly, ConsoleColor.DarkBlue);
					}
				}
			}

			return options;
		}
	}
}

