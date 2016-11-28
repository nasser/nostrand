(ns nostrand.bootstrap)

(defn full-aot []
  (binding [*compile-path* "."]
    (compile 'nostrand.core)
    (compile 'nostrand.tasks)
    (compile 'nostrand.repl)))