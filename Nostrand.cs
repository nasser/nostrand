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
		public static ISeq ReadArguments(string[] args)
		{
			ISeq list = new PersistentList.EmptyList(null);

			var argString = string.Join(" ", args.Reverse());
			var pbtr = new PushbackTextReader(new StringReader(argString));
			for (;;)
			{
				try
				{
					list = list.cons(ArgumentReader.read(pbtr, true, null, false, null));
				}
				catch (EndOfStreamException)
				{
					return list;
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
					var v = Namespace.find(Symbol.intern(taskNS)).FindInternedVar(Symbol.intern(taskVarName));
					return v;
				}
				else
				{
					// namespace not given, check tasks
					var tasksVar = Namespace.find(Symbol.intern("nostrand.tasks")).FindInternedVar(Symbol.intern(name));
					if (tasksVar != null)
						return tasksVar;
					else
					{
						var coreVar = Namespace.find(Symbol.intern("clojure.core")).FindInternedVar(Symbol.intern(name));
						if (coreVar != null)
							return coreVar;
						
					}
					return null;
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
				RT.load("nostrand/core");
				RT.load("nostrand/tasks");

				var input = ReadArguments(args);
				IFn fn = FindFunction(input.first().ToString());

				if (fn == null)
				{
					Terminal.Message("Quiting", "could not find function named `" + args[0] + "'", ConsoleColor.Yellow);
					return;
				}

				fn.applyTo(input.next());
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

