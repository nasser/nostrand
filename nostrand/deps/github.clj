(assembly-load-with-partial-name "System.IO.Compression.FileSystem")
(assembly-load-with-partial-name "System.IO.Compression")

(ns nostrand.deps.github
  (:import [System.IO.Compression ZipFile]
           [System.IO Directory File]
           [System.Net WebClient HttpRequestHeader])
  (:require [nostrand.deps :refer [acquire! paths assemblies]]
            [nostrand.deps.shell :refer [sh in-dir]]))

(defn build-uri
  "Create the proper uri to fetch the remote lib."
  [branch token user repo sha]
  (cond
    (and token user repo sha) ;; private repo
    (str "https://api.github.com/repos/" user "/" repo "/zipball/" sha)

    (and user repo branch sha) ;; public repo via sha
    (str "https://github.com/" user "/" repo "/archive/" sha ".zip")

    (and user repo branch) ;; public repo via default branch
    (str "https://github.com/" user "/" repo "/archive/" branch ".zip")

    :else
    (throw (ex-info "Missing some GitHab API parameters:"
                    (if token
                      {:scope :private :user user :repo repo :branch branch :sha sha}
                      {:scope :public :user user :repo repo :branch branch :sha sha})))))

(defn archive-repo-format
  "Returns the expected repo name in the archive. "
  [github-repo github-user branch sha token]
  (cond
    token
    (str github-user "-" github-repo "-" sha)
    sha
    (str github-repo "-" sha)
    :else
    (str github-repo "-" branch)))

(defmethod acquire! :github
  [{:keys [root]}
   [head repo branch & {:keys [sha token]}]]
  (let [github-user       (namespace repo)
        github-repo       (name repo)
        url               (build-uri branch token github-user github-repo sha)
        prefix            (str root "/" (name head) "/" github-user)
        temp-name         (str (gensym (str github-user "-" github-repo)) ".zip")
        repo-mame-final   (str github-repo "-" branch)
        repo-name-fetched (archive-repo-format github-repo github-user branch sha token)
        web-client        (WebClient.)]
    (println "Downloading GitHub Repo: " repo)
    (when-not (Directory/Exists (str prefix "/" repo-mame-final))
      (in-dir prefix
              (when token
                (. (.Headers web-client) (Add "User-Agent" "My App"))
                (. (.Headers web-client) (Add HttpRequestHeader/Authorization (str "token " token))))
              (. web-client (DownloadFile url temp-name))
              (ZipFile/ExtractToDirectory temp-name ".")
              (File/Delete temp-name)
              (when-not (= repo-name-fetched repo-mame-final)
                (Directory/Move (str "./" repo-name-fetched)
                                (str "./" repo-mame-final)))))))

(defmethod paths :github
  [{:keys [root]}
   [head repo branch & {:keys [paths]}]]
  (let [github-user (namespace repo)
        github-repo (name repo)
        prefix (str root "/" (name head) "/" github-user "/" github-repo "-" branch)]
    (concat [prefix]
            (map #(str prefix "/" %) paths))))
