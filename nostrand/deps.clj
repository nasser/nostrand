(ns nostrand.deps
  (:import [System.IO Directory]))

;;; syntax
; (depend
;   [[nasser/tools.analyzer.clr "1.0"]
;    [:maven nasser/tools.analyzer.clr "1.0"]
;    [:nuget OpenTK "2.0.0"]
;    [:npm express]
;    [:github nasser/magic "master"]
;    [:git "https://github.com/nasser/magic.git" "master"]
;    [:gx QmR5FHS9TpLbL9oYY8ZDR3A7UWcHTBawU1FJ6pu9SvTcPa]])
; (depend
;   [[nasser/tools.analyzer.clr "1.0"]
;    [nasser/tools.analyzer.clr "1.0" :source :maven]
;    [OpenTK "2.0.0" :source :nuget]
;    [express :source :npm]
;    [nasser/magic "master" :source :github]
;    ["https://github.com/nasser/magic.git" "master" :source :git]
;    [QmR5FHS9TpLbL9oYY8ZDR3A7UWcHTBawU1FJ6pu9SvTcPa :source :gx]])
;;; api
;; (acquire! [opts coords]) -> nil (downloads deps)
;; (dependencies [opts coords]) -> [coord...] (coords of packages transitive deps)
;; (paths [opts coords]) -> [""] (paths to clojure namespace roots/assemblies)

;; (reference System.IO.Compression.FileSystem
;;            deps/nuget/OpenTK.2.0.0/lib/net20/OpenTK.dll)

(def ^:dynamic *options* {:root "deps"})

(defmulti acquire!
  (fn [opts coord] (first coord)))

(defmulti paths
  (fn [opts coord] (first coord)))

(defn assemblies [opts coord]
  (let [lib-paths (paths opts coord)]
    (mapcat #(Directory/GetFiles % "*.dll") lib-paths)))