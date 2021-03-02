(ns
    ^{:author "Ramsey Nasser"
      :doc "Core nostrand API containing load path, assemblies, and dependency functions."}
    nostrand.core
  (:require [clojure.string :as string]
            [nostrand.deps :as deps]
            nostrand.deps.github
            nostrand.deps.gitlab
            nostrand.deps.maven
            nostrand.deps.nuget)
  (:import [System.IO Path File]))

(def -assembly-path
  (atom (string/split (or (Environment/GetEnvironmentVariable "MONO_PATH") ".")
                      (re-pattern (str Path/PathSeparator)))))

(def -load-path
  (atom (string/split (or (Environment/GetEnvironmentVariable "CLOJURE_LOAD_PATH") ".")
                      (re-pattern (str Path/PathSeparator)))))

(defn resolve-assembly-load [asm]
  (let [candidates (for [prefix @-assembly-path
                         ext ["" ".dll" ".exe"]]
                     ;; asm contains more info than just the name itself.
                     (let [file-name (-> asm (string/split (re-pattern (str ","))) first)]
                       (Path/Combine prefix (str file-name ext))))
        full-asm-path (first (filter #(File/Exists %) candidates))]
    (when full-asm-path
      (assembly-load-from full-asm-path))))

(defn update-load-path []
  (Environment/SetEnvironmentVariable
    "CLOJURE_LOAD_PATH"
    (string/join Path/PathSeparator @-load-path))
  (alter-var-root #'*load-paths*
                  (fn [load-paths]
                    (mapv
                     #(System.IO.Path/GetFullPath %)
                     (concat @-load-path load-paths)))))

(defn set-load-path [val]
  (reset! -load-path val)
  (update-load-path))

(defn add-load-path [path]
  (swap! -load-path conj path)
  (update-load-path))

(defn add-assembly-path [path]
  (swap! -assembly-path conj path))



(defn load-path [& paths]
  (doseq [p paths]
    (add-load-path p)))

(defn assembly-path [& paths]
  (doseq [p paths]
    (add-assembly-path p)))

(defn reference* [asms]
  (doseq [asm asms]
    (let [a (str asm)]
      (assembly-load-from a))))

(defmacro reference [& asms]
  `(reference* ~(mapv str asms)))

(defn depend*
  [opts coord]
  (deps/acquire! opts coord)
  (doseq [path (deps/paths opts coord)]
    (add-load-path path))
  (reference* (deps/assemblies opts coord)))

(defmacro depend [coords]
  `(do
     ~@(map (fn [coord] `(depend* deps/*options* '~coord))
            coords)))

(defn establish-environment [{:keys [source-paths assembly-paths dependencies references]
                              :as config}]
  (def configuration config)
  (when source-paths
    (apply load-path source-paths))
  (when assembly-paths
    (apply assembly-path assembly-paths))
  (when dependencies
    (doseq [d dependencies]
      (depend* deps/*options* d)))
  (when references
    (reference* references)))
