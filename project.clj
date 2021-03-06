(def tk-version "1.1.1")
(def ks-version "1.1.0")

(defn deploy-info
  [url]
  {:url url
   :username :env/nexus_jenkins_username
   :password :env/nexus_jenkins_password
   :sign-releases false})

(defproject puppetlabs/pcp-broker "0.2.2-SNAPSHOT"
  :description "PCP fabric messaging broker"
  :url "https://github.com/puppetlabs/pcp-broker"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  ;; Abort when version ranges or version conflicts are detected in
  ;; dependencies. Also supports :warn to simply emit warnings.
  ;; requires lein 2.2.0+.
  :pedantic? :abort

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]

                 ;; Transitive dependency for puppetlabs/trapperkeeper-metrics and org.slf4j/jcl-over-slf4
                 [org.slf4j/slf4j-api "1.7.10"]

                 [puppetlabs/trapperkeeper ~tk-version]
                 [puppetlabs/kitchensink ~ks-version]
                 [puppetlabs/trapperkeeper-webserver-jetty9 "1.4.0"]
                 [puppetlabs/trapperkeeper-metrics "0.1.1"]

                 [cheshire "5.5.0"]
                 [prismatic/schema "0.4.3"]
                 [clj-time "0.9.0"]

                 [com.taoensso/nippy "2.9.0"]

                 [org.clojure/java.jmx "0.3.0"]
                 [metrics-clojure "2.5.1"]

                 ;; try+/throw+
                 [slingshot "0.12.2"]

                 [puppetlabs/pcp-common "0.4.1"]

                 ;; MQ - activemq
                 [clamq/clamq-activemq "0.4"]
                 [org.apache.activemq/activemq-core "5.6.0"
                  :exclusions [org.fusesource.fuse-extra/fusemq-leveldb]]
                 ;; bridge to allow some spring/activemq stuff to log over slf4j
                 [org.slf4j/jcl-over-slf4j "1.7.10"]]

  :plugins [[lein-release "1.0.5" :exclusions [org.clojure/clojure]]]

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots"  "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :deploy-repositories [["releases" ~(deploy-info "http://nexus.delivery.puppetlabs.net/content/repositories/releases/")]
                        ["snapshots" ~(deploy-info "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/")]]

  :lein-release {:scm :git, :deploy-via :lein-deploy}

  :test-paths ["test" "test-resources"]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[http.async.client "0.6.1" :exclusions [org.clojure/clojure]]
                                  [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                                  [puppetlabs/trapperkeeper ~tk-version :classifier "test" :scope "test"]
                                  [puppetlabs/kitchensink ~ks-version :classifier "test" :scope "test"]
                                  [puppetlabs/ssl-utils "0.8.1"]
                                  [me.raynes/fs "1.4.5"]
                                  [org.clojure/tools.namespace "0.2.4"]]}
             :cljfmt {:plugins [[lein-cljfmt "0.3.0"]
                                [lein-parent "0.2.1"]]
                      :parent-project {:path "../pl-clojure-style/project.clj"
                                       :inherit [:cljfmt]}}}

  :repl-options {:init-ns user}

  ;; Enable occasionally to check we have no interop hotspots that need better type hinting
  ; :global-vars {*warn-on-reflection* true}

  :aliases {"tk" ["trampoline" "run" "--config" "test-resources/conf.d"]
            "certs" ["trampoline" "run" "-m" "puppetlabs.pcp.testutils.certs" "--config" "test-resources/conf.d" "--"]
            "cljfmt" ["with-profile" "+cljfmt" "cljfmt"]}

  :main puppetlabs.trapperkeeper.main)
