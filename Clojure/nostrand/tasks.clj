(ns
  ^{:author "Ramsey Nasser"
    :doc "Built in nostrand tasks, available from the command line as unqualified functions"}
  nostrand.tasks
  (:import
    [Nostrand Nostrand]
    [System.Threading Thread ThreadStart]
    [System.Reflection AssemblyInformationalVersionAttribute])
  (:require [nostrand.repl :as repl]))

(defn- msg
  ([header body]
   (Nostrand.Terminal/Message header body))
  ([header body color]
   (Nostrand.Terminal/Message header body color)))

(defn version []
  (msg "Nostrand" (Nostrand/Version))
  (msg "Mono" (Nostrand/GetMonoVersion))
  (msg "Clojure" (clojure-version)))

(defn cli-repl [args]
  (repl/cli args))

(defn socket-repl [args]
  (repl/socket args))

(defn repl
  ([]
   (version)
   (repl/repl 11217))
  ([port]
   (version)
   (repl/repl port)))
