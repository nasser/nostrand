using System;

namespace Nostrand
{
	public class FunctionAttribute : Attribute
	{
		public readonly string Name;
		public FunctionAttribute(string name)
		{
			Name = name;
		}
	}
}