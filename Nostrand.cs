using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Threading;
using System.Reflection;
using System.Runtime.InteropServices;
using clojure.lang;

namespace Nostrand
{

	public class Nostrand
	{
		public struct Input
		{
			public IPersistentMap options;
			public PersistentVector functions;
		}

		public static Input ReadArguments(string[] args)
		{
			var input = new Input();
			input.options = PersistentHashMap.EMPTY;
			input.functions = PersistentVector.EMPTY;

			var argString = string.Join(" ",args);
			var pbtr = new PushbackTextReader(new StringReader(argString));
			for (;;)
			{
				try
				{
					var o = ArgumentReader.read(pbtr, true, null, false, null);
					if (o is Keyword)
					{
						var val = ArgumentReader.read(pbtr, true, null, false, null);
						input.options = input.options.assoc(o, val);
					}
					else if(o is Symbol)
					{
						input.functions = (PersistentVector)input.functions.cons(o);
					}
				}
				catch (EndOfStreamException)
				{
					return input;
				}
			}
		}

		public static IFn FindFunction(string name)
		{
			// TODO stop using C# functions, they complicate things
			var tasks = Tasks("mscorlib", "Clojure");
			Type taskType;
			if (tasks.TryGetValue(name, out taskType))
			{
				// c# task
				return (IFn)Activator.CreateInstance(taskType);
			}

			// clojure task
			try
			{
				if (name.Contains("/"))
				{
					var taskName = name;
					var taskParts = taskName.Split('/');
					var taskNS = taskParts[0];
					var taskVarName = taskParts[1];
					RT.load(taskNS.Replace('.', '/'));
					return RT.var(taskNS, taskVarName);
				}
			}
			catch (NullReferenceException)
			{

			}

			return null;
		}

		[DllImport("__Internal", EntryPoint = "mono_get_runtime_build_info")]
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
			var tasks = types.Where(type => type.GetCustomAttribute<FunctionAttribute>() != null).
							 ToDictionary(type => type.GetCustomAttribute<FunctionAttribute>().Name);
			return tasks;
		}

		public static void Main(string[] args)
		{
			if (args.Length > 0)
			{
				new Thread(() =>
				{
					RT.load("clojure/core");
					RT.load("clojure/repl");
				}).Start();

				var input = ReadArguments(args);
				object arg = input.options;

				var builtinFunctions = PersistentVector.create(new SetLoadPathFunction(), new LoadAssembliesFunction());

				foreach (var f in builtinFunctions)
				{
					arg = ((IFn)f).invoke(arg);
				}

				foreach (var f in input.functions)
				{
					IFn fn = FindFunction(f.ToString());
					if (fn == null)
					{
						Terminal.Message("Quiting", "could not find function named `" + args[0] + "'", ConsoleColor.Yellow);
						return;
					}

					arg = fn.invoke(arg);
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

