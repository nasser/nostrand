# Nostrand
Standalone runtime environment and REPL for ClojureCLR on Mono.

## Status
Very early, pre-alpha. Everything will change. Don't use for anything critical.

## Installing

1. clone the `nostrand` repo

```
$ git clone https://github.com/nasser/nostrand.git
```

2. Fetch the last `magic` dlls

Go to [nasser/magic/actions](https://github.com/nasser/magic/actions).

Click on the last worflow run, scroll down and download the artifact `magic-assemblies`.

Copy the dlls inside the `nostrand/references` folder.

3. Build and REPL

```
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

```clojure
$ cat tasks.clj
(ns tasks)

(defn build []
  (binding [*compile-path* "build"]
    (compile 'important.core)
    (compile 'important.util)))

$ nos tasks/build
```

Command line arguments are parsed as EDN passed to the function.

```clojure
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

```clojure
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

Nostrand supports packages from Maven Central and Clojars, as well as github and NuGet. `project.edn`'s `:dependencies` key accepts a vector of package coordinates of the form `[source name version]` , where `source` is a keyword that specifies which repository to pull from, `name` is a symbol that specifies the name of the package, and `version` is a string that specifies the verison of the package. `name` and `version` will depend on the `source`.

For example, the [MAGIC project's](https://github.com/nasser/magic) [`project.edn`](https://github.com/nasser/magic/blob/master/project.edn) contains:

```clojure
:dependencies [[:github nasser/mage "master"]
               [:github nasser/test.check "master"]
               [:maven org.clojure/tools.analyzer "0.6.9"]]
```

#### `:github`
Github sources clone a whole repository as a dependency.

- `name` takes the form `username/repository` and `version` is anything that refers to a commit (sha recommended).
- You must provide both `branch` and `sha` if you want to download the zip of a specific branch.
- For private repo, the `token` must be provided and the `sha`.
- Be careful with GitHub personal token has they cannot be set to read only! (For your private GitHub repo, it is advised to create a dummy account with read-only access on your repos and generate the token from this dummy user.)

```clojure
[;; public repo
 [:github skydread1/my-public-lib "feature-1"
  :sha "46bb12e33ae4fe118a1aa91d2985f1f2f3192366"
  :paths ["src"]]
 ;; private repo
 [:github skydread1/my-private-lib "master"
  :paths ["src"]
  :sha "46bb12e33ae4fe118a1aa91d2985f1f2f3192367"
  :token "xxxxxxxxxxxxx"]]
```

#### `:gitlab`
Gitlab sources clone a whole repository as a dependency.

- `name` takes the form `username/repository` and `version` is  anything that refers to a commit (sha recommended).
- The `branch`, `sha` and the `project-id` must be provided for public and private repository.
- For private repository, `domain` and access `token` are required as well.

```clojure
[;; public repo
 [:gitlab skydread1/my-public-lib "master"
  :paths ["src"]
  :sha "46bb12e33ae4fe118a1aa91d2985f1f2f3192366"
  :project-id "777"]
 ;; private repo
 [:gitlab skydread1/my-private-lib "master"
  :paths ["src"]
  :sha "46bb12e33ae4fe118a1aa91d2985f1f2f3192367"
  :token "xxxxxxxxxxxxx"
  :domain "dev.hello.sg"
  :project-id "888"]]
```

The root of the repository is the only directory added to the load path by default, but you can specify subdirectories by adding `:paths` followed by a vector of strings to the coordinate.

#### `:nuget`
`name` and `version` are the same as you would pass in to `nuget install name -Version version`.

You can pack and push packages easily with the convenient function `tasks/nuget-push`.

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
