using System;
using System.Linq;
using System.Collections.Generic;
using System.Text.RegularExpressions;
using Mono.Terminal;
using clojure.lang;

namespace Nostrand
{
	[Task("repl")]
	public class ReplTask : AFn
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

		public override object invoke()
		{
			LineEditor le = new LineEditor("nostrand");
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

			Var.pushThreadBindings(
				RT.mapUniqueKeys(RT.CurrentNSVar, Namespace.findOrCreate(Symbol.intern("user")),
				RT.WarnOnReflectionVar, RT.WarnOnReflectionVar.deref(),
				RT.UncheckedMathVar, RT.UncheckedMathVar.deref()));

			do
			{
				try
				{
					var readResult = RT.var("clojure.core", "read-string").invoke(s);
					var evaledResult = RT.var("clojure.core", "eval").invoke(readResult);
					var stringResult = RT.var("clojure.core", "pr-str").invoke(evaledResult).ToString();
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