(ns devdemos.lots-of-data
  (:require [reagent.core :as reagent]
            [reabledit.core :as reabledit]
            [reabledit.components :as components])
  (:require-macros [devcards.core :as dc :refer [defcard-rg]]))

(def rows 100)
(def cols 30)

(def example-keys (map #(-> % str keyword) (range 1 cols)))

(defn generate-row
  []
  (reduce #(assoc %1 %2 (str (gensym))) {} example-keys))

(def example-columns (mapv #(assoc {} :key % :value (str %)) example-keys))
(def example-data (mapv generate-row (range 1 50)))

(defonce example-atom (reagent/atom example-data))

(defn example-row-fn
  [nth-row old-row new-row]
  (swap! example-atom reabledit/update-row nth-row new-row))

(defn data-table*
  [data-atom]
  [reabledit/data-table example-columns @data-atom example-row-fn])

(defcard-rg reabledit
  (fn [data-atom _]
    [data-table* data-atom])
  example-atom)
