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
		public override object invoke(object argMap)
		{
			var path = ((IPersistentMap)argMap).valAt(Keyword.intern("path"));
			if (path == null)
				return null;

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
			return null;
		}
	}
}

