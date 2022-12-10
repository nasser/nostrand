(ns
    ^{:author "Ramsey Nasser"
      :doc    "Built in nostrand tasks, available from the command line as unqualified functions"}
    nostrand.tasks
  (:import
   [Nostrand Nostrand]
   [System.IO Directory]
   [System.Threading Thread ThreadStart]
   [System.Reflection AssemblyInformationalVersionAttribute])
  (:require [nostrand.repl :as repl]
            [nostrand.deps.nuget :as nuget]
            [clojure.string :as string]
            [clojure.core.server :as clj-server]
            clojure.repl))

(defn- msg
  ([header body]
   (Nostrand.Terminal/Message header body))
  ([header body color]
   (Nostrand.Terminal/Message header body color)))

(defn version []
  (msg "Nostrand" (Nostrand/Version))
  (msg "Clojure.Runtime" (Nostrand/ClojureRuntimeVersion))
  (msg "Magic.Runtime" (Nostrand/MagicRuntimeVersion))
  (msg "Clojure" (clojure-version)))

(defn cli-repl
  ([] (cli-repl nil))
  ([args]
   (repl/cli args)))

(defn socket-repl [args]
  (repl/socket args))

(defn repl
  ([]
   (version)
   #_ (repl/repl 11217)
   (cli-repl))
  ([port]
   (version)
   #_ (repl/repl port)
   (cli-repl)))

(defn tasks []
  (let [ns-syms
        (->> (Directory/GetFiles "." "*.clj")
             (map #(-> %
                       (string/replace "./" "")
                       (string/replace ".clj" "")
                       symbol)))]
    (doseq [s ns-syms]
      (require s)
      (let [fns (->> s
                     find-ns
                     ns-publics
                     vals)]
        (doseq [f fns]
          ((var clojure.repl/print-doc) (meta f)))))))


(defn clojure-socket-repl [args]
  (print "Starting Clojure socket repl...")
  (let [opts (merge
              {:accept `clj-server/repl
               :name "Clojure socket repl"}
              args)]
    (clj-server/start-server opts)
    (println "done ")
    (println "Started socket repl with Options: " opts)
    (repl)))

(defn nuget-push
  "Pack and Push NuGet Package to git host repo.
  - `git-host`     : 'github' or 'gitlab'
  - `with-build?`  : true by default
  - `configuration`: 'Release' by default."
  [git-host-type with-build? configuration]
  (binding [*compile-path* "build"]
    (nuget/pack-and-push-nuget git-host-type
                               :with-build? with-build?
                               :configuration configuration)))
