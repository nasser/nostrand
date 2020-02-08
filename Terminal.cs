using System;
namespace Nostrand
{
	public class Terminal
	{

		public static void Message(string content, ConsoleColor color)
		{
			Message(null, content, color);
		}

		public static void Message(string label, object content) {
			Message (label, content, ConsoleColor.White);
		}

		public static void Message(string label, object content, ConsoleColor color) {
			var oldColor = Console.ForegroundColor;
			Console.ForegroundColor = color;
			if(label != null)
				Console.Write (label + " ");
			Console.ForegroundColor = ConsoleColor.Gray;
			Console.WriteLine (content.ToString());
			//Console.ForegroundColor = oldColor;
			Console.ResetColor ();
			// due to some bug in mono we need to compute the cursor offset
			// ourselves otherwise Mono.Terminal breaks
			var fullMessage = label != null ? "${label} ${content}" : content.ToString();
			int newLines = 0;
			foreach (var line in fullMessage.Split(new char['\n']))
			{
				newLines += 1 + (line.Length / Console.WindowWidth);
			}
			Console.SetCursorPosition(0, Math.Min(Console.CursorTop+newLines, Console.WindowHeight-1));
		}
	}
}

