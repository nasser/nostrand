using System;

namespace Nostrand
{
	public class TaskAttribute : Attribute
	{
		public readonly string Name;
		public TaskAttribute(string name)
		{
			Name = name;
		}
	}
}