# Nostrand
Standalone runtime environment and REPL for ClojureCLR on Mono.

## Status
Very early, pre-alpha. Everything will change. Don't use for anything critical.

## Installing

```
$ git clone https://github.com/nasser/nostrand.git
$ cd nostrand
$ xbuild
$ ln -s ./bin/Debug/nos ~/bin/
$ nos repl
user>
```

## Usage

```
nos FUNCTION [ARGUMENT...]
```

Nostrand does one thing: it runs a function. A function is either a Clojure function, or defined in C#. Some functions are built in.

```
$ nos version
Nostrand 0.0.1.33392 (master/9e61e2f* Wed Jun 22 18:33:04 EDT 2016)
Mono 4.4.1 (mono-4.4.0-branch-c7sr0/4747417 Mon Jun 20 15:43:48 EDT 2016)
Clojure 1.7.0-master-SNAPSHOT

$ nos repl
user> 
```

In the future, you will be able to provide Nostrand with compiled functions written in C#.

But Nostrand is primarly meant to run namespace-qualified Clojure functions.

```
$ cat foo.clj
(ns foo)
(defn bar [] (dotimes [i 5] (println (str "foobar: " i))))
$ nos foo/bar
foobar: 0
foobar: 1
foobar: 2
foobar: 3
foobar: 4
```

Additional command line arguments are passed to the function.

```
$ cat foo.clj
(ns foo)
(defn bar [i]
  (dotimes [j (int i)] (println (str "foobar: " j))))
$ nos foo/bar 10
foobar: 0
foobar: 1
foobar: 2
foobar: 3
foobar: 4
foobar: 5
foobar: 6
foobar: 7
foobar: 8
foobar: 9
```  

The load path is the current directory. Future releases will allow dependency and load path management.

## Name
[Nostrand Avenue](https://en.wikipedia.org/wiki/Nostrand_Avenue) is a major street in Brooklyn near where I was living when I began the project.

## Legal
Copyright © 2016 Ramsey Nasser

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

```
http://www.apache.org/licenses/LICENSE-2.0
```

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.