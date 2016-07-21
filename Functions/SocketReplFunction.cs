using System;
using System.Collections.Generic;
using System.Text;
using System.Net;
using System.Net.Sockets;
using System.IO;
using System.Linq;
using clojure.lang;

namespace Nostrand
{
	[Function("socket-repl")]
	public class SocketReplFunction : AFn
	{
		const int DefaultPort = 11217;

		string FormatResponse(string s)
		{
			return string.Format("{0}\n{1}> ", s, ((Namespace)RT.CurrentNSVar.deref()).Name);
		}

		string FormatResponse(Exception e)
		{
			return string.Format("\n{0}\n{1}> ", e, ((Namespace)RT.CurrentNSVar.deref()).Name);
		}

		public override object invoke()
		{
			return StartRepl(DefaultPort);
		}

		public override object invoke(object argMap)
		{
			var portArg = ((IPersistentMap)argMap).valAt(Keyword.intern("port")) ?? (long)DefaultPort;
			int port = (int)(long)portArg;
			return StartRepl(port);
		}

		object StartRepl(int port)
		{
			var socket = new UdpClient(new IPEndPoint(IPAddress.Any, port));
			socket.Client.SendBufferSize = 1024 * 5000;
			socket.Client.ReceiveBufferSize = 1024 * 5000;

			Terminal.Message("Listening", port);

			var readStringFn = (IFn)RT.var("clojure.core", "read-string").getRawRoot();
			var evalFn = (IFn)RT.var("clojure.core", "eval").getRawRoot();
			var prStrFn = (IFn)RT.var("clojure.core", "pr-str").getRawRoot();

			Var.pushThreadBindings(
				RT.mapUniqueKeys(
					RT.CurrentNSVar, Namespace.findOrCreate(Symbol.intern("user")),
					RT.WarnOnReflectionVar, RT.WarnOnReflectionVar.deref(),
					RT.UncheckedMathVar, RT.UncheckedMathVar.deref()));

			while (true)
			{
				var sender = new IPEndPoint(IPAddress.Any, 0);
				var inBytes = socket.Receive(ref sender);
				if (inBytes.Length > 0)
				{
					try
					{
						var code = Encoding.UTF8.GetString(inBytes);
						var readResult = readStringFn.invoke(code);
						var evaledResult = evalFn.invoke(readResult);
						var stringResult = prStrFn.invoke(evaledResult).ToString();
						var outBytes = Encoding.UTF8.GetBytes(FormatResponse(stringResult));
						socket.Send(outBytes, outBytes.Length, sender);
					}
					catch (Exception e)
					{
						var exceptionBytes = Encoding.UTF8.GetBytes(FormatResponse(e));
						socket.Send(exceptionBytes, exceptionBytes.Length, sender);
					}
				}
			}
		}
	}
}

