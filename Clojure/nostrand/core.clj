;; AVAILABLE IN NS FORM

;; TODO rename to e.g. nostrand.patch?
(in-ns 'clojure.core)

(defn get-load-path []
  (Environment/GetEnvironmentVariable
    "CLOJURE_LOAD_PATH"))

(defn set-load-path [val]
  (Environment/SetEnvironmentVariable
    "CLOJURE_LOAD_PATH"
    val))

(set-load-path ".")

(defn get-mono-path []
  (Environment/GetEnvironmentVariable
    "MONO_PATH"))

(defn set-mono-path [val]
  (Environment/SetEnvironmentVariable
    "MONO_PATH"
    val))

(defn add-load-path [path]
  (set-load-path
    (str (get-load-path)
         Path/PathSeparator
         (if-not (.StartsWith (str path) "/")
           (str "./" path)
           path))))

;; TODO same as add-load-path?
(defn add-assemblies-path [path]
  (set-mono-path
    (str (get-mono-path)
         Path/PathSeparator
         path)))

(defn load-path [& paths]
  (doseq [p paths]
    (add-load-path p)))

(defn assemblies-path [& paths]
  (doseq [p paths]
    (add-assemblies-path p)))

(defn load-assemblies [& asms]
  (doseq [a asms]
    (assembly-load a)))