(ns nostrand.bootstrap)

(defn full-aot []
  (binding [clojure.core/*loaded-libs* (ref (sorted-set))]
    (compile 'nostrand.core)
    (compile 'nostrand.tasks)
    (compile 'nostrand.repl)))