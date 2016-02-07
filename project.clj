(defproject reagent-spreadsheet "0.1.0-SNAPSHOT"
  :description "Minimal Reagent component that mimics spreadsheet applications like Excel"
  :url "https://github.com/MattiNieminen/reagent-spreadsheet"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [reagent "0.6.0-alpha"]]
  :clean-targets ^{:protect false} ["example-resources/public/js/compiled"
                                    :target-path]
  :cljsbuild
  {:builds
   [{:id "devcards"
     :source-paths ["example-src" "src"]
     :figwheel {:devcards true}
     :compiler {:main "devdemo.core"
                :asset-path "js/compiled/devcards_out"
                :output-to "example-resources/public/js/compiled/reagent-spreadsheet_devcards.js"
                :output-dir "example-resources/public/js/compiled/devcards_out"
                :optimizations :none
                :source-map-timestamp true}}]}
  :profiles {:dev
             {:dependencies [[devcards "0.2.1-6"]]
              :plugins [[lein-figwheel "0.5.0-6"]]
              :resource-paths ["resources" "example-resources"]}})
