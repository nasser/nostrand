(assembly-load-with-partial-name "System.IO.Compression.FileSystem")
(assembly-load-with-partial-name "System.IO.Compression")

(ns nostrand.deps.github
  (:import [System.IO.Compression ZipFile]
           [System.IO Directory File]
           [System.Net WebClient])
  (:require [nostrand.deps :refer [acquire! paths assemblies]]
            [nostrand.deps.shell :refer [sh in-dir]]))

(defmethod acquire! :github
  [{:keys [root] :as opts}
   [head repo branch & coord-opts]]
  (let [github-user (namespace repo)
        github-repo (name repo)
        url (str "https://github.com/" github-user "/" github-repo "/archive/" branch ".zip")
        prefix (str root "/" (name head) "/" github-user)
        temp-name (str (gensym (str github-user "-" github-repo)) ".zip")]
    (when-not (Directory/Exists (str prefix "/" github-repo "-" branch))
      (in-dir prefix
              (. (WebClient.) (DownloadFile url temp-name))
              (ZipFile/ExtractToDirectory temp-name ".")
              (File/Delete temp-name)))))

(defmethod paths :github
  [{:keys [root] :as opts}
   [head repo branch & {:keys [paths] :as coord-opts}]]
  (let [github-user (namespace repo)
        github-repo (name repo)
        prefix (str root "/" (name head) "/" github-user "/" github-repo "-" branch)]
    (concat [prefix]
            (map #(str prefix "/" %) paths))))