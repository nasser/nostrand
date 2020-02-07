(assembly-load-with-partial-name "System.IO.Compression.FileSystem")
(assembly-load-with-partial-name "System.IO.Compression")
(assembly-load-with-partial-name "System.Xml")

(ns nostrand.deps.maven
  (:require [nostrand.deps :refer [acquire! paths assemblies]]
            [clojure.clr.io :as io]
            [clojure.string :as string])
  (:import [System.Xml XmlReader XmlNodeType]
           [System.Text.RegularExpressions Regex]
           [System.Net WebClient WebException WebRequest HttpWebRequest]
           [System.IO.Compression ZipFile ZipArchive ZipArchiveEntry ZipArchiveMode ZipFileExtensions]
           [System.IO Directory DirectoryInfo Path File FileInfo FileSystemInfo StringReader Path]))

;; xml reading
(defmulti xml-content (fn [^XmlReader xml] (.NodeType xml)))
(defmethod xml-content
  :default [^XmlReader xml]
  nil)

(defmethod xml-content
  XmlNodeType/Text [^XmlReader xml]
  (string/trim (.Value xml)))

(defmethod xml-content
  XmlNodeType/Element [^XmlReader xml]
  (keyword (.Name xml)))

(defn read-xml-from-reader [^XmlReader xml]
  (loop [accumulator []]
    (let [depth (.Depth xml)]
      (.Read xml)
      (cond (or (.EOF xml)
                (= (.NodeType xml)
                   XmlNodeType/EndElement))
            (seq (filter identity accumulator)) ;; remove nils?
            (.IsEmptyElement xml)
            (recur accumulator)
            :else (if (> (.Depth xml)
                         depth)
                    (recur (conj accumulator
                                 (xml-content xml)
                                 (read-xml-from-reader xml)))
                    (recur (conj accumulator
                                 (xml-content xml))))))))

(defn read-xml [source]
  (read-xml-from-reader (XmlReader/Create (StringReader. source))))

(def flatten-set #{:licenses :dependencies :exclusions :resources :testResources :repositories})

(defn make-map [stream]
  (if (seq? stream)
    (reduce (fn [acc [k v]]
              (assoc acc k (if (flatten-set k)
                             (mapv make-map (take-nth 2 (drop 1 v)))
                             (make-map v))))
            {}
            (partition 2 stream))
    stream))

(def temp-dir "temp")

(defmacro in-dir [dir & body]
  `(let [original-cwd# (Directory/GetCurrentDirectory)]
     (try
       (do (Directory/CreateDirectory ~dir)
           (Directory/SetCurrentDirectory ~dir)
           ~@body)
       (finally
         (Directory/SetCurrentDirectory original-cwd#)))))

;; TODO dont hardcode
(def url-prefixes
  ["http://central.maven.org/maven2/"
   "http://search.maven.org/remotecontent?filepath="
   "https://oss.sonatype.org/content/repositories/snapshots/"
   "https://repo1.maven.org/maven2/"
   "https://clojars.org/repo/"])

(defn url-exists? [^String url]
  (let [req ^HttpWebRequest (WebRequest/Create url)]
    (try
      (set! (.Method req) "HEAD")
      (set! (.Timeout req) 1000)
      (.GetResponse req)
      true
      (catch WebException c
        false))))

(defn short-base-url [group artifact version]
  (let [group (string/replace group "." "/")]
   (str group "/"
        artifact "/"
        version "/")))

(defn download-file [url file]
  (with-open [wc (WebClient.)]
    (try (.. wc (DownloadFile url file))
      file
      (catch System.Net.WebException e
        nil))))

(defn download-string [url]
  (with-open [wc (WebClient.)]
    (try
      (.. wc (DownloadString url))
      (catch System.Net.WebException e
        nil))))

(defn base-url [group artifact version]
  (str (short-base-url group artifact version)
       artifact "-"
       version))

(defn base-metadata-url [group artifact version]
  (str (short-base-url group artifact version) "maven-metadata.xml"))

(defn metadata-urls [group artifact version]
  (->> (map #(str % (base-metadata-url group artifact version)) url-prefixes)
       (filter url-exists?)))

(defn metadata-url [group artifact version]
  (first (metadata-urls group artifact version)))

(defn snapshot-timestamp
  [group artifact version]
  (if-let [metadata (metadata-url group artifact version)]
    (->> (download-string metadata)
         read-xml
         make-map
         :metadata
         :versioning
         :snapshot
         ((juxt :timestamp :buildNumber))
         (string/join "-"))))

(defn base-jar-url [group artifact version]
  (string/replace (str (base-url group artifact version) ".jar")
                  #"-SNAPSHOT.jar"
                  (str "-" (snapshot-timestamp group artifact version) ".jar")))

(defn base-pom-url [group artifact version]
  (string/replace (str (base-url group artifact version) ".pom")
                  #"-SNAPSHOT.pom"
                  (str "-" (snapshot-timestamp group artifact version) ".pom")))

(defn jar-urls [group artifact version]
  (->> (map #(str % (base-jar-url group artifact version)) url-prefixes)
       (filter url-exists?)))

(defn pom-urls [group artifact version]
  (->> (map #(str % (base-pom-url group artifact version)) url-prefixes)
       (filter url-exists?)))

(defn jar-url [group artifact version]
  (first (jar-urls group artifact version)))

(defn pom-url [group artifact version]
  (first (pom-urls group artifact version)))

(defn dependencies
  [[group artifact version]]
  (let [pom (pom-url group artifact version)]
    (->> (download-string pom)
         read-xml 
         make-map
         :project
         :dependencies
         (remove #(= (% :scope) "test"))
         (remove #(= (% :artifactId) "clojure"))
         (map (juxt :groupId :artifactId :version)))))

(defn all-dependencies [[group artifact version]]
  (into #{}
        (apply concat
               (take-while seq (iterate #(mapcat dependencies %)
                                        (dependencies [group artifact version]))))))

(defn most-recent-versions [deps]
  (->> (group-by (juxt first second) deps)
     vals
     (map (fn [vs] (->> vs
                     (sort-by last)
                     last)))))

(defn all-unique-dependencies [group-artifact-version]
  (-> group-artifact-version
      all-dependencies
      most-recent-versions))

(defn- path-combine [& paths]
  (letfn [(as-path [x]
            (if (instance? FileSystemInfo x)
              (.FullName x)
              x))
          (step
            ([] "")
            ([p] p)
            ([p1 p2]
             (Path/Combine p1 p2)))]
    (transduce (map as-path) step paths)))

(defn download-jar
  [[group artifact version]]
  (Console/WriteLine (str "Downloading " group "-" artifact "-" version "-"))
  (let [temp (DirectoryInfo. (path-combine temp-dir "jars"))]
    (when-not (.Exists temp) (.Create temp))
    (Path/GetFullPath
      (download-file
        (jar-url group artifact version)
        (path-combine (.FullName temp)
          (str (gensym (str group "-" artifact "-" version "-")) ".jar"))))))

(defn download-jars
  [group-artifact-version]
  (doall (->> (all-unique-dependencies group-artifact-version)
              (concat [group-artifact-version])
              (map download-jar))))

(defn should-extract? [^ZipArchiveEntry e]
  (not (or (re-find #"^META-INF" (.FullName e))
           (re-find #"^project\.clj" (.FullName e))
           (re-find #"/$" (.FullName e)))))

(def dir-seperator-re
  (re-pattern (Regex/Escape (str Path/DirectorySeparatorChar))))

(defn make-directories [^ZipArchiveEntry e base]
  (let [dir (->> (string/split (.FullName e) dir-seperator-re)
                 (drop-last 1)
                 (string/join (str Path/DirectorySeparatorChar))
                 (conj [base])
                 (string/join (str Path/DirectorySeparatorChar)))]
    (Directory/CreateDirectory dir)
    dir))

(defn- as-directory ^DirectoryInfo [x]
  (if (instance? DirectoryInfo x)
    x
    (DirectoryInfo. x)))

(defn normalize-coordinates [group-artifact-version]
  (case (count group-artifact-version)
    3 (map str group-artifact-version)
    2 (let [[group-artifact version] group-artifact-version]
        [(or (namespace group-artifact)
             (name group-artifact))
         (name group-artifact)
         version])
    (throw (Exception. (str "Package coordinate must be a vector with 2 or 3 elements, got " group-artifact-version)))))

(defn- extract [coord base]
  (Console/WriteLine (str "Extracting " coord))
  (let [jar (download-jar coord)
        archive (ZipFile/Open jar ZipArchiveMode/Read)
        extractable-entries (filter should-extract? (.Entries archive))]
    (doseq [^ZipArchiveEntry zip-entry extractable-entries]
      (make-directories zip-entry base)
      (ZipFileExtensions/ExtractToFile zip-entry (str base Path/DirectorySeparatorChar (.FullName zip-entry)) true))
    coord))

(defn- blocked-coordinate? [coord installed]
  (let [[group artifact version] (normalize-coordinates coord)]
    (or (= artifact "clojure")
        (contains? installed (normalize-coordinates coord)))))

(defn install
  [group-artifact-version base]
  (Console/WriteLine (str "Installing " group-artifact-version))
  (let [gav (normalize-coordinates group-artifact-version)]
    (->> (cons gav (all-dependencies gav))
         most-recent-versions
         (mapv #(extract % base)))))

(defmethod acquire! :maven 
  [{:keys [root] :as opts}
   [head id version & coord-opts]]
  (let [prefix (str root "/" (name head) "/" id "-" version)]
    (when-not (Directory/Exists prefix)
      (install [id version] prefix))))

(defmethod paths :maven 
  [{:keys [root] :as opts} 
   [head id version & opts]]
  [(str root "/" (name head) "/" id "-" version)])
