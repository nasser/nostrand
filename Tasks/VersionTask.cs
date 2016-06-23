using clojure.lang;
namespace Nostrand
{
	[Task("version")]
	public class VersionTask : ITask
	{
		public void Invoke(string[] args)
		{
			Terminal.Message("Nostrand", Nostrand.Version());
			Terminal.Message("Mono", Nostrand.GetMonoVersion());
			Terminal.Message("Clojure", RT.var("clojure.core", "clojure-version").invoke());
		}
	}
}

