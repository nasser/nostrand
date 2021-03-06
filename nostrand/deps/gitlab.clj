(assembly-load-with-partial-name "System.IO.Compression.FileSystem")
(assembly-load-with-partial-name "System.IO.Compression")

(ns nostrand.deps.gitlab
  (:import [System.IO.Compression ZipFile]
           [System.IO Directory File]
           [System.Net WebClient])
  (:require [nostrand.deps :refer [acquire! paths]]
            [nostrand.deps.shell :refer [in-dir]]))

(defn build-uri
  "Create the proper uri to fetch the remote lib."
  [branch token domain project-id]
  (if token
    ;; private project : needs token access and domain
    (str "https://" domain "/api/v4/projects/" project-id "/repository/archive.zip?ref=" branch "&access_token=" token)
    ;; public project
    (str "https://gitlab.com/api/v4/projects/" project-id "/repository/archive.zip?ref=" branch)))

(defmethod acquire! :gitlab
  [{:keys [root] :as opts}
   [head repo branch & {:keys [sha token domain project-id]}]]
  (let [gitlab-user (namespace repo)
        gitlab-repo (name repo)
        url (build-uri branch token domain project-id)
        prefix (str root "/" (name head) "/" gitlab-user)
        temp-name (str (gensym (str gitlab-user "-" gitlab-repo)) ".zip")]
    (when-not (Directory/Exists (str prefix "/" gitlab-repo "-" branch "-" sha))
      (in-dir prefix
              (. (WebClient.) (DownloadFile url temp-name))
              (ZipFile/ExtractToDirectory temp-name ".")
              (File/Delete temp-name)))))

(defmethod paths :gitlab
  [{:keys [root]}
   [head repo branch & {:keys [paths sha]}]]
  (let [gitlab-user (namespace repo)
        gitlab-repo (name repo)
        prefix (str root "/" (name head) "/" gitlab-user "/" gitlab-repo "-" branch "-" sha)]
    (concat [prefix]
            (map #(str prefix "/" %) paths))))
