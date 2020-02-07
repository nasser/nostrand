(ns nostrand.bootstrap)

(defn full-aot []
  (binding [*compile-path* "."
            *compile-files* true]
    (require 'nostrand.core :reload-all)
    (require 'nostrand.tasks :reload-all)
    (require 'nostrand.repl :reload-all)))