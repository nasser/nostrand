(ns nostrand.tasks
  (:import
    [Nostrand Nostrand]
    [System.Threading Thread ThreadStart]
    [System.Reflection AssemblyInformationalVersionAttribute])
  (:require [nostrand.repl :as repl]))

;; AVAILABLE IN COMMAND LINE

(defn- msg
  ([header body]
   (Nostrand.Terminal/Message header body))
  ([header body color]
   (Nostrand.Terminal/Message header body color)))

(defn version [args]
  (msg "Nostrand" (Nostrand/Version))
  (msg "Mono" (Nostrand/GetMonoVersion))
  (msg "Clojure" (clojure-version))
  args)

;; TODO repls

(defn cli-repl [args]
  (repl/cli args))

(defn socket-repl [args]
  (repl/socket args))

(defn repl [args]
  (version args)
  (repl/repl args))