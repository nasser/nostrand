using System;
using System.Linq;
using System.Collections.Generic;
using System.Text.RegularExpressions;
using Mono.Terminal;
using clojure.lang;

namespace Nostrand
{
	[Function("cli-repl")]
	public class ReplFunction : AFn
	{
		bool firstPrompt = true;
		string Prompt()
		{
			if (firstPrompt)
			{
				firstPrompt = false;
				return "user> ";
			}

			return ((Namespace)RT.CurrentNSVar.deref()).Name.Name + "> ";
		}

		public static Keyword historySizeKw = Keyword.intern("repl", "history");

		public override object invoke(object argMap)
		{
			var args = (IPersistentMap)argMap;
			long historySize = 500;
			var historyArg = args.valAt(historySizeKw);
			if (historyArg != null)
			{
				historySize = (long)historyArg;
			}
			LineEditor le = new LineEditor("nostrand", (int)historySize);
			le.AutoCompleteEvent += (string prefix, int pos) =>
			{
				prefix = Regex.Match(prefix, "([^\\(\\)]+)$").ToString();
				var completions = ((IEnumerable<object>)RT.var("clojure.repl", "apropos").
								   invoke(new Regex("^" + prefix))).Select((sym) => ((Symbol)sym).Name).
																   Select((str) => str.Substring(prefix.Length)).
																   ToArray();

				return new LineEditor.Completion(prefix, completions);
			};

			string s;
			s = le.Edit(Prompt(), "");


			var readStringFn = (IFn)RT.var("clojure.core", "read-string").getRawRoot();
			var evalFn = (IFn)RT.var("clojure.core", "eval").getRawRoot();
			var prStrFn = (IFn)RT.var("clojure.core", "pr-str").getRawRoot();

			Var.pushThreadBindings(
				RT.mapUniqueKeys(RT.CurrentNSVar, Namespace.findOrCreate(Symbol.intern("user")),
				RT.WarnOnReflectionVar, RT.WarnOnReflectionVar.deref(),
				RT.UncheckedMathVar, RT.UncheckedMathVar.deref()));

			do
			{
				try
				{
					var readResult = readStringFn.invoke(s);
					var evaledResult = evalFn.invoke(readResult);
					var stringResult = prStrFn.invoke(evaledResult).ToString();
					Terminal.Message(stringResult, ConsoleColor.Gray);
				}
				catch (System.IO.EndOfStreamException)
				{
					// dont throw on blank input
				}
				catch (Exception e)
				{
					Terminal.Message("Exception", e.ToString(), ConsoleColor.Yellow);
				}
			} while ((s = le.Edit(Prompt(), "")) != null);

			Var.popThreadBindings();

			return null;
		}
	}
}