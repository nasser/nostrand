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
			var list = PersistentVector.EMPTY; //PersistentList.EmptyList(null);

			var argString = string.Join(" ", args);
			var pbtr = new PushbackTextReader(new StringReader(argString));
			for (;;)
			{
				try
				{
					list = (PersistentVector)list.cons(ArgumentReader.read(pbtr, true, null, false, null));
				}
				catch (EndOfStreamException)
				{
					return list.seq();
				}
			}
		}

		public static Var FindFunction(string name)
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

		// [DllImport("__Internal", EntryPoint = "mono_get_runtime_build_info")]
		// public extern static string GetRuntimeVersion();

		static string GetVersionString(Assembly asm)
		{
			return ((AssemblyInformationalVersionAttribute)(asm.GetCustomAttributes(typeof(AssemblyInformationalVersionAttribute), false)[0])).InformationalVersion;
		}

		public static string Version()
		{
			var asm = typeof(Nostrand).Assembly;
			if (Assembly.Load("System").GetName().Version.Major == 2)
				return asm.GetName().Version.ToString();
			return GetVersionString(asm);
		}

		public static string MagicRuntimeVersion()
		{
			return GetVersionString(typeof(Magic.Runtime).Assembly);
		}

		public static string ClojureRuntimeVersion()
		{
			return GetVersionString(typeof(clojure.lang.RT).Assembly);
		}

		public static string FileToRelativePath(string file)
		{
			return file.Replace(".clj", "").Replace(".cljc", "");
		}

		static void BootClojureAndNostrand()
		{
			var assemblyPath = Path.GetDirectoryName(Assembly.Load("Clojure").Location);
			foreach(var cljDll in Directory.EnumerateFiles(assemblyPath, "*.clj.dll"))
			{
				Assembly.LoadFile(cljDll);
			}

			RT.Initialize(doRuntimePostBoostrap: false);
			RT.TryLoadInitType("clojure/core");
			RT.TryLoadInitType("magic/api");

			RT.var("clojure.core", "*load-fn*").bindRoot(RT.var("clojure.core", "-load"));
			RT.var("clojure.core", "*eval-form-fn*").bindRoot(RT.var("magic.api", "eval"));
			RT.var("clojure.core", "*load-file-fn*").bindRoot(RT.var("magic.api", "runtime-load-file"));
			RT.var("clojure.core", "*compile-file-fn*").bindRoot(RT.var("magic.api", "runtime-compile-file"));
			RT.var("clojure.core", "*macroexpand-1-fn*").bindRoot(RT.var("magic.api", "runtime-macroexpand-1"));

			// var loadFunction = RT.var("clojure.core", "*load-fn*");
			// loadFunction.invoke("nostrand/core");
			RT.var("clojure.core", "*load-fn*").invoke("nostrand/core");
			// loadFunction.invoke("nostrand/tasks");
			RT.var("clojure.core", "*load-fn*").invoke("nostrand/tasks");
		}

		public static void Main(string[] args)
		{
			new Mono.Terminal.LineEditor("#force-mono.terminal-assembly-load#");
			BootClojureAndNostrand();
			if (args.Length > 0)
			{
				AppDomain.CurrentDomain.AssemblyResolve += AssemblyResolver.Resolve;

				RT.var("nostrand.core", "load-path").invoke(Directory.GetCurrentDirectory());
				
				if (File.Exists("project.edn"))
				{
					var projectEdn = EdnReader.readString(File.ReadAllText("project.edn"), PersistentHashMap.EMPTY);
					RT.var("nostrand.core", "establish-environment").invoke(projectEdn);
				}

				RT.PostBootstrapInit();

				var input = ReadArguments(args);
				var inputString = input.first().ToString();
				if (inputString.IndexOf("./", StringComparison.InvariantCulture) == 0)
					inputString = inputString.Substring(2);

				Var.pushThreadBindings(RT.mapUniqueKeys(RT.CurrentNSVar, Namespace.find(Symbol.intern("nostrand.core"))));
				try
				{
					Var fn = FindFunction(inputString);

					if (fn != null)
					{
						//referAll.invoke(fn.Namespace, nostrandCore);
						fn.applyTo(input.next());
						return;
					}
					if (File.Exists(inputString))
					{
						try
						{
							IFn mainFn = FindFunction(FileToRelativePath(inputString) + "/-main");
							if (mainFn != null)
							{
								mainFn.applyTo(input.next());
							}
							return;
						}
						catch (FileNotFoundException)
						{
						}
					}

					Terminal.Message("Quiting", "could not find function or file named `" + args[0] + "'", ConsoleColor.Yellow);
				}
				finally
				{
					Var.popThreadBindings();
				}
			}

			else
			{
				Terminal.Message("Nostrand", Version(), ConsoleColor.White);
				// Terminal.Message("Mono", GetRuntimeVersion(), ConsoleColor.White);
				Terminal.Message("Clojure", RT.var("clojure.core", "clojure-version").invoke(), ConsoleColor.White);
			}
		}
	}
}

