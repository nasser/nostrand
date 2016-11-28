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
  (msg "Nostrand"
       (let [asm (Assembly/Load "Nostrand")
             version (.. asm GetName Version)
             version-info (-> asm
                              (.GetCustomAttributes
                                AssemblyInformationalVersionAttribute
                                true)
                              first
                              .InformationalVersion)]
         (str version " " version-info)))
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