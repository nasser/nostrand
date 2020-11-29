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
			Console.ResetColor ();
		}
	}
}

