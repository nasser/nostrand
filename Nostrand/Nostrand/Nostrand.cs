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

		static Dictionary<string, Type> Tasks()
		{
			var mscorlibAssembly = Assembly.Load("mscorlib");
			var clojureAssembly = Assembly.Load("Clojure");
			var assemblies = AppDomain.CurrentDomain.GetAssemblies().Where((assembly) => assembly != mscorlibAssembly && assembly != clojureAssembly);
			var types = assemblies.SelectMany((assembly) => assembly.GetTypes());
			var tasks = types.Where((type) => type.GetCustomAttribute<TaskAttribute>() != null).
							 ToDictionary((type) => type.GetCustomAttribute<TaskAttribute>().Name);
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

				var tasks = Tasks();
				Type taskType;
				if (tasks.TryGetValue(args[0], out taskType))
				{
					// c# task
					var task = (ITask)Activator.CreateInstance(taskType);
					task.Invoke(args.Skip(1).ToArray());
				}
				else
				{
					// clojure task
					try
					{
						var taskName = args[0];
						var taskParts = taskName.Split('/');
						var taskNS = taskParts[0];
						var taskVar = taskParts[1];
						RT.load(taskNS.Replace('.', '/'));
						RT.var(taskNS, taskVar).invoke();
					}
					catch (System.NullReferenceException)
					{

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

