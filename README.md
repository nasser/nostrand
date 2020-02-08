# Nostrand
Standalone runtime environment and REPL for ClojureCLR on Mono.

## Status
Very early, pre-alpha. Everything will change. Don't use for anything critical.

## Installing

```
$ git clone https://github.com/nasser/nostrand.git
$ cd nostrand
$ dotnet build -c Release
$ ln -s `pwd`/bin/x64/Release/net471/nos ~/bin/
$ nos repl
Nostrand 0.2.1.0 (master/4115065 Sat 08 Feb 2020 05:50:17 PM EST)
Mono 6.4.0 (makepkg/fe64a4765e6 Sat 16 Nov 2019 04:59:42 PM UTC)
Clojure 1.10.0-master-SNAPSHOT
REPL 0.0.0.0:11217
user>
```

## Usage

```
nos FUNCTION [ARG...]
```

Nostrand does one thing: it runs a function.

Functions are Clojure functions. Without a namespace, they resolve to the `nostrand.tasks` namespace which is built in.

```
$ nos version
Nostrand 0.0.1.6940 (master/a1e4260* Mon Nov 28 03:51:20 EST 2016)
Mono 4.8.0 (mono-4.8.0-branch/f5fbc32 Mon Nov 14 14:18:03 EST 2016)
Clojure 1.7.0-master-SNAPSHOT

$ nos repl
Nostrand 0.0.1.6940 (master/a1e4260* Mon Nov 28 03:51:20 EST 2016)
Mono 4.8.0 (mono-4.8.0-branch/f5fbc32 Mon Nov 14 14:18:03 EST 2016)
Clojure 1.7.0-master-SNAPSHOT
REPL 0.0.0.0:11217
user>
```

With a namespace they are searched for using Clojure's normal namespace resolution machinery. The current directory is on the load path by default.

```
$ cat tasks.clj
(ns tasks)

(defn build []
  (binding [*compile-path* "build"]
    (compile 'important.core)
    (compile 'important.util)))

$ nos tasks/build
```

Command line arguments are parsed as EDN passed to the function.

```
$ cat tasks.clj
(ns tasks)

(defn build [utils?]
  (binding [*compile-path* "build"]
    (compile 'important.core)
    (when utils?
      (compile 'important.util))))

$ nos tasks/build true
```

Your entry namespace can also set up your classpath, load assemblies, and eventually manage dependencies.  

```
$ cat tasks.clj
(assembly-load-from "assemblies/SomeLib.dll")
(ns tasks
  (:import [SomeLib SomeType]))

(defn build [utils?]
  (SomeType/DoThing)
  (binding [*compile-path* "build"]
    (compile 'important.core)
    (when utils?
      (compile 'important.util))))

$ nos tasks/build true
```

### `project.edn`
If a file named `project.edn` is present in the current directory, it will be parsed at startup to configure initialization. It is expected to be an EDN map, and the following keys are recognized:

* `:source-paths` A vector of paths to load Clojure namespaces from. The default is the current directory.
* `:assembly-paths` A vector of paths to load assemblies from.
* `:references` A vector of assembly names to reference.
* `:dependencies` A vector of package coordinates your project depends on

### Dependencies
*Support for dependencies is very new and likely buggy. Please take out issues as you encounter them.*

Nostrand supports packages from Maven Central and Clojars, as well as github and NuGet. `project.edn`'s `:dependencies` key accepts a vector of package coordinates of the form `[source name version]` , where `source` is a keyword that specifies which repository to pull from, `name` is a symbol that specifies the name of the package, and `version` is a string that specifies the verison of the package. `name` and `version` will depend on the `source`.

For example, the [MAGIC project's](https://github.com/nasser/magic) [`project.edn`](https://github.com/nasser/magic/blob/master/project.edn) contains:

```clojure
:dependencies [[:github nasser/mage "master"]
               [:github nasser/test.check "master"]
               [:maven org.clojure/tools.analyzer "0.6.9"]]
```

#### `:github`
Github sources clones a whole repository as a dependency. `name` takes the form `username/repository` and `version` is anything that refers to a commit, like a branch name, a tag, or a full commit hash.

The root of the repository is the only directory added to the load path by default, but you can specify subdirectories by adding `:paths` followed by a vector of strings to the coordinate.

#### `:nuget`
`name` and `version` are the same as you would pass in to `nuget install name -Version version`.

#### `:maven`
Behaves as in Leiningen. Note that many Clojure packages will not work on ClojurCLR without modification, as they may reference Java classes that are not present on the CLR.

## Name
[Nostrand Avenue](https://en.wikipedia.org/wiki/Nostrand_Avenue) is a major street and subway stop in Brooklyn near where I was living when I began the project.

## Legal
Copyright © 2016-2017 Ramsey Nasser

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

```
http://www.apache.org/licenses/LICENSE-2.0
```

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
