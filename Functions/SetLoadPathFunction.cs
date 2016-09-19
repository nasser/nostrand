using System;
using System.Text;
using System.IO;
using System.Collections.Generic;
using System.Linq;
using clojure.lang;

namespace Nostrand
{
	public class SetLoadPathFunction : AFn
	{
		public override object invoke(object options)
		{
			var verbose = ((IPersistentMap)options).valAt(Keyword.intern("verbose"));
			var path = ((IPersistentMap)options).valAt(Keyword.intern("load-path"));
			if (path == null)
				return options;

			if (path is string ||
			    path is Symbol)
				Environment.SetEnvironmentVariable("CLOJURE_LOAD_PATH", path.ToString());

			else if (path is IEnumerable<object>)
			{
				var pathList = (IEnumerable<object>)path;
				var loadPath =
					pathList.Skip(1).Aggregate(new StringBuilder(pathList.First().ToString()),
								   (sb, p) => sb.Append(Path.PathSeparator)
					                            .Append(p.ToString())).ToString();
				Environment.SetEnvironmentVariable("CLOJURE_LOAD_PATH", loadPath);
			}

			if (verbose != null && ((bool)verbose) == true)
			{
				Terminal.Message("Load Path", Environment.GetEnvironmentVariable("CLOJURE_LOAD_PATH"), ConsoleColor.DarkBlue);
			}
			return options;
		}
	}
}

