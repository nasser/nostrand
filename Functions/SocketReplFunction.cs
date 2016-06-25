using System;
using System.Text;
using System.Net;
using System.Net.Sockets;
using System.IO;
using clojure.lang;

namespace Nostrand
{
	[Function("socket-repl")]
	public class SocketReplFunction : AFn
	{

		string FormatResponse(string s)
		{
			return string.Format("{0}\n{1}> ", s, ((Namespace)RT.CurrentNSVar.deref()).Name);
		}

		string FormatResponse(Exception e)
		{
			return string.Format("\n{0}\n{1}> ", e, ((Namespace)RT.CurrentNSVar.deref()).Name);
		}

		public override object invoke(object portArg, object fnName)
		{
			IFn fn = Nostrand.FindFunction((string)fnName);
			if (fn != null)
			{
				fn.invoke();
			}

			return invoke(portArg);
		}

		public override object invoke(object portArg)
		{
			int port = int.Parse((string)portArg);
			var socket = new UdpClient(new IPEndPoint(IPAddress.Any, port));
			socket.Client.SendBufferSize = 1024 * 5000;
			socket.Client.ReceiveBufferSize = 1024 * 5000;

			Terminal.Message("Listening", port);

			var outWriter = new StringWriter();
			var readStringFn = (IFn)RT.var("clojure.core", "read-string").getRawRoot();
			var evalFn = (IFn)RT.var("clojure.core", "eval").getRawRoot();
			var prStrFn = (IFn)RT.var("clojure.core", "pr-str").getRawRoot();

			Var.pushThreadBindings(
				RT.mapUniqueKeys(
					RT.CurrentNSVar, Namespace.findOrCreate(Symbol.intern("user")),
					RT.OutVar, outWriter,
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
						var printedString = outWriter.ToString();
						outWriter.GetStringBuilder().Clear();
						if (printedString.Length > 0)
						{
							var printedBytes = Encoding.UTF8.GetBytes(printedString);
							socket.Send(printedBytes, printedBytes.Length, sender);
						}

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

