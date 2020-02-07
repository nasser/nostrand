(ns nostrand.deps.shell
  (:import [System.IO Directory Path])
  (:require [clojure.clr.shell :as shell]))

(def sh shell/sh)

(defn require-command [cmd]
  (when (= (-> (sh "which" cmd) :exit) 1)
    (throw (Exception. (str cmd " command not found")))))

(def setdir  #(Directory/SetCurrentDirectory %))
(def makedir #(Directory/CreateDirectory %))
(def getdir  #(Directory/GetCurrentDirectory))

(defmacro in-dir [dir & body]
 `(let [cwd# (getdir)]
    (try (makedir ~dir) (setdir ~dir) ~@body
         (finally (setdir cwd#)))))

(defn path-combine [path]
  (Path/Combine (into-array (map str path))))