using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Reflection;
using System.Runtime.InteropServices;
using clojure.lang;

namespace Nostrand
{

	public class Nostrand
	{

		[DllImport("__Internal", EntryPoint="mono_get_runtime_build_info")]
		public extern static string GetMonoVersion();

		public static string Version()
		{
			var asm = Assembly.GetCallingAssembly();
			return asm.GetName().Version + " " + asm.GetCustomAttribute<AssemblyInformationalVersionAttribute>().InformationalVersion;
		}

		static Dictionary<string, Type> Tasks(params string[] ignoring)
		{
			var ignoredAssemblies = ignoring.Select(s => Assembly.Load(s));
			var assemblies = AppDomain.CurrentDomain.GetAssemblies().Where(assembly => !ignoredAssemblies.Contains(assembly));
			var types = assemblies.SelectMany(assembly => assembly.GetTypes());
			var tasks = types.Where(type => type.GetCustomAttribute<TaskAttribute>() != null).
							 ToDictionary(type => type.GetCustomAttribute<TaskAttribute>().Name);
			return tasks;
		}

		public static void Main(string[] args)
		{
			if (args.Length > 0)
			{
				new Thread(() => {
					RT.load("clojure/core");
					RT.load("clojure/repl");
				}).Start();

				IFn fn = null;

				var tasks = Tasks("mscorlib", "Clojure");
				Type taskType;
				if (tasks.TryGetValue(args[0], out taskType))
				{
					// c# task
					fn = (IFn)Activator.CreateInstance(taskType);
				}
				else
				{
					// clojure task
					try
					{
						if (args[0].Contains("/"))
						{
							var taskName = args[0];
							var taskParts = taskName.Split('/');
							var taskNS = taskParts[0];
							var taskVarName = taskParts[1];
							RT.load(taskNS.Replace('.', '/'));
							fn = RT.var(taskNS, taskVarName);
						}
					}
					catch (NullReferenceException)
					{

					}
				}

				if (fn == null)
				{
					Terminal.Message("Quiting", "could not find function named `" + args[0] + "'", ConsoleColor.Yellow);
				}
				else
				{

					if (args.Length > 1)
					{
						AFn.ApplyToHelper(fn, RT.seq(args.Skip(1)));
					}
					else
					{
						fn.invoke();
					}
				}

			}
			else
			{
				Terminal.Message("Nostrand", Version());
				Terminal.Message("Mono", GetMonoVersion());
				Terminal.Message("Clojure", RT.var("clojure.core", "clojure-version").invoke());
			}
		}
	}
}

