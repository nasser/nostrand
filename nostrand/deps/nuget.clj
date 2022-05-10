(ns nostrand.deps.nuget
  (:import [System Version]
           [System.IO Path Directory]
           [System.Reflection AssemblyFileVersionAttribute])
  (:require [nostrand.deps :refer [acquire! paths assemblies]]
            [nostrand.deps.shell :refer [sh in-dir path-combine require-command]]))

;; TODO non-.NET Frameworks? .NET Standard?
(defn moniker->version [moniker]
  (let [[_ major minor build] (re-find #"net(\d)(\d)?(\d)?" moniker)
        minor (if (= minor "") "0" minor)
        build (if (= build "") "0" build)]
    (Version. major minor build)))

(defn version->moniker
  ([version] (version->moniker version "net"))
  ([version name]
   (str name
        (.Major version)
        (.Minor version)
        (when (pos? (.Build version))
          (.Build version)))))

(defn current-framework-version []
  (let [system-version
        (-> "System"
            assembly-load
            (.GetCustomAttributes AssemblyFileVersionAttribute false)
            first
            .Version)
        [_ major minor build]
        (re-find #"^([0-9]+)\.([0-9]+)\.([0-9])" system-version)
        build (if (= build "") "0" build)]
    (Version. major minor build)))

(defn latest-useable-verion [versions target-version]
  (->> versions
       sort
       (take-while
        #(Version/op_LessThanOrEqual % target-version))
       last))

(defn latest-useable-moniker [monikers target-version]
  (->> monikers
       sort ;; ??
       (take-while
        #(Version/op_LessThanOrEqual (moniker->version %) target-version))
       last))

(defn supported-framework-monikers [lib-folder]
  (->> lib-folder
       Directory/GetDirectories
       (map #(Path/GetFileName %))
       (filter #(re-find #"^net\d+$" %))))

;; TODO replace w/nuget library
(defmethod acquire! :nuget
  [{:keys [root] :as opts}
   [head id version & coord-opts]]
  (require-command "nuget")
  (let [prefix (str root "/" (name head))]
    (when-not (Directory/Exists prefix)
      (in-dir prefix
              (sh "nuget" (str "install " id " -Version " version))))))

(defmethod paths :nuget
  [{:keys [root] :as opts}
   [head id version & coord-opts]]
  (let [libs-path (path-combine [root (name head) (str id "." version) "lib"])
        supported-monikers (->> libs-path
                                supported-framework-monikers)
        best-moniker (latest-useable-moniker
                      supported-monikers
                      (current-framework-version))
        lib-path (path-combine [libs-path best-moniker])]
    [lib-path]))

;;; Pack and Push NuGet Packages

(defn pack-and-push-nuget
  "Packs the dlls and push it to the online repo.
   prerequisite: you need to add at the root of the project:
   - A `.csproj`     : dependency manager
   - A `.nuspec`     : package manager
   - A `nuget.config`: host repo credentials (keep this file locally)"
  [git-host-type & {:keys [with-build? configuration]
                    :or   {with-build? true configuration "Release"}}]
  (println "packing...")
  (let [{:keys [exit out err]} (sh "dotnet" (str "pack --configuration " configuration
                                                 (when-not with-build? " --no-build")))]
    (when (= 1 exit)
      (throw (ex-info "error while packing" {:out out :err err}))))
  (println "pushing to " git-host-type "...")
  (let [{:keys [exit out err]} (sh "dotnet"
                                   (str "nuget push bin/" configuration "/*.nupkg "
                                        "--source " git-host-type))]
    (when (= 1 exit)
      (throw (ex-info "error while pushing" {:out out :err err})))))
