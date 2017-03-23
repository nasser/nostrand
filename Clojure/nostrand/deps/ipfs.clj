(ns nostrand.deps.ipfs
  (:import [System Version]
           [System.Reflection AssemblyFileVersionAttribute])
  (:require [nostrand.deps.shell :refer [sh]]))

(defmethod acquire! :ipfs
  [{:keys [root] :as opts}
   [head hsh & coord-opts]]
  (require-command "ipfs")
  (let [prefix (str root "/" (name head))]
    (in-dir prefix
            (sh "ipfs" (str "get " hsh)))))