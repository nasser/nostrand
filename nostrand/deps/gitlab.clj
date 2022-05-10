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
  [token domain project-id sha]
  (cond
    (and token domain project-id sha) ;; private repo
    (str "https://" domain "/api/v4/projects/" project-id "/repository/archive.zip?sha=" sha "&access_token=" token)

    (and project-id sha) ;; public repo
    (str "https://gitlab.com/api/v4/projects/" project-id "/repository/archive.zip?sha=" sha)

    :else
    (throw (ex-info "Missing some GitLab API parameters: "
                    (if token
                      {:scope :private :domain domain :project-id project-id :sha sha}
                      {:scope :public :project-id project-id :sha sha})))))

(defn archive-repo-format
  "Returns the expected repo name in the archive."
  [gitlab-repo branch sha]
  (if sha
    (str gitlab-repo "-" sha "-" sha)
    (str gitlab-repo "-" branch "-" sha)))

(defmethod acquire! :gitlab
  [{:keys [root]}
   [head repo branch & {:keys [sha token domain project-id]}]]
  (let [gitlab-user       (namespace repo)
        gitlab-repo       (name repo)
        url               (build-uri token domain project-id sha)
        prefix            (str root "/" (name head) "/" gitlab-user)
        temp-name         (str (gensym (str gitlab-user "-" gitlab-repo)) ".zip")
        repo-mame-final   (str gitlab-repo "-" branch)
        repo-name-fetched (archive-repo-format gitlab-repo branch sha)]
    (println "Downloading GitLab Repo: " repo)
    (when-not (Directory/Exists (str prefix "/" repo-mame-final))
      (in-dir prefix
              (. (WebClient.) (DownloadFile url temp-name))
              (ZipFile/ExtractToDirectory temp-name ".")
              (File/Delete temp-name)
              (Directory/Move (str "./" repo-name-fetched)
                              (str "./" repo-mame-final))))))

(defmethod paths :gitlab
  [{:keys [root]}
   [head repo branch & {:keys [paths]}]]
  (let [gitlab-user (namespace repo)
        gitlab-repo (name repo)
        prefix (str root "/" (name head) "/" gitlab-user "/" gitlab-repo "-" branch)]
    (concat [prefix]
            (map #(str prefix "/" %) paths))))
