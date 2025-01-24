(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'net.clojars.danielmiladinov/burpless)
;; Be sure to also keep the version in README up to date with this
(def version "1.0.0-alpha1")
#_ ; alternatively, use MAJOR.MINOR.COMMITS:
(def version (format "1.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")

(defn test "Run all the tests." [opts]
  (let [basis    (b/create-basis {:aliases [:test]})
        cmds     (b/java-command
                  {:basis      basis
                    :main      'clojure.main
                    :main-args ["-m" "cognitect.test-runner"]})
        {:keys [exit]} (b/process cmds)]
    (when-not (zero? exit) (throw (ex-info "Tests failed" {}))))
  opts)

(defn- pom-template [version]
  [[:description "An idiomatic Clojure wrapper around cucumber-jvm, for writing Cucumber feature tests."]
   [:url "https://github.com/danielmiladinov/burpless"]
   [:licenses
    [:license
     [:name "APACHE LICENSE, VERSION 2.0"]
     [:url "https://www.apache.org/licenses/LICENSE-2.0.txt"]]]
   [:developers
    [:developer
     [:name "Daniel Miladinov"]]]
   [:scm
    [:url "https://github.com/danielmiladinov/burpless"]
    [:connection "scm:git:https://github.com/danielmiladinov/burpless.git"]
    [:developerConnection "scm:git:ssh:git@github.com:danielmiladinov/burpless.git"]
    [:tag (str "v" version)]]])

(defn- jar-opts [opts]
  (assoc opts
          :lib lib   :version version
          :jar-file  (format "target/%s-%s.jar" (name lib) version)
          :basis     (b/create-basis {})
          :class-dir class-dir
          :target    "target"
          :src-dirs  ["src"]
          :pom-data  (pom-template version)))

(defn clean [opts]
  (b/delete {:path "target"})
  opts)

(defn ci
  "Run the CI pipeline of tests (and build the JAR)."
  [opts]
  (test opts)
  (b/delete {:path "target"})
  (let [opts (jar-opts opts)]
    (println "\nWriting pom.xml...")
    (b/write-pom opts)
    (println "\nCopying source...")
    (b/copy-dir {:src-dirs ["resources" "src"] :target-dir class-dir})
    (println "\nBuilding JAR..." (:jar-file opts))
    (b/jar opts))
  opts)

(defn install "Install the JAR locally." [opts]
  (let [opts (jar-opts opts)]
    (b/install opts))
  opts)

(defn deploy
  "Deploy the JAR to Clojars."
  [opts]
  (let [{:keys [jar-file] :as opts} (jar-opts opts)
        deploy-args{:installer :remote :artifact (b/resolve-path jar-file)
                    :pom-file (b/pom-path (select-keys opts [:lib :class-dir]))}]
       (println "deploy args:\n")
       (println (str deploy-args))
    (dd/deploy deploy-args))
  opts)
