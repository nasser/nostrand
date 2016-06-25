using clojure.lang;
namespace Nostrand
{
	[Function("version")]
	public class VersionFunction : AFn
	{
		public override object invoke()
		{
			Terminal.Message("Nostrand", Nostrand.Version());
			Terminal.Message("Mono", Nostrand.GetMonoVersion());
			Terminal.Message("Clojure", RT.var("clojure.core", "clojure-version").invoke());

			return null;
		}
	}
}

