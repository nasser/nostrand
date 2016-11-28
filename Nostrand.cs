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
				else
				{
					return RT.var("nostrand.tasks", name);
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
			var asm = typeof(Nostrand).Assembly;
			return asm.GetName().Version + " " + asm.GetCustomAttribute<AssemblyInformationalVersionAttribute>().InformationalVersion;
		}

		public static void Main(string[] args)
		{
			if (args.Length > 0)
			{
				RT.load("clojure/core");

				var input = ReadArguments(args);
				object arg = input.options;

				RT.load("nostrand/core");
				RT.load("nostrand/tasks");

				foreach (var f in input.functions)
				{
					IFn fn = FindFunction(f.ToString());
					if (fn == null)
					{
						Terminal.Message("Quiting", "could not find function named `" + args[0] + "'", ConsoleColor.Yellow);
						return;
					}

					try
					{
						arg = fn.invoke(arg);
					}
					catch (ArityException)
					{
						arg = fn.invoke();
					}
				}
			}

			else
			{
				Terminal.Message("Nostrand", Version(), ConsoleColor.White);
				Terminal.Message("Mono", GetMonoVersion(), ConsoleColor.White);
				Terminal.Message("Clojure", RT.var("clojure.core", "clojure-version").invoke(), ConsoleColor.White);
			}
		}
	}
}

