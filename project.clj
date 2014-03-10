(defproject madnet "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev {:dependencies [[khazad-dum "0.2.0"]]
                   :repl-options {:init (use 'khazad-dum)}}}
  :source-paths ["src/"]
  :java-source-paths ["java/"]
  :dependencies [[org.clojure/clojure "1.5.1"]])
