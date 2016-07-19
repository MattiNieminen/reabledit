(ns devdemos.lots-of-data
  (:require [reagent.core :as reagent]
            [reabledit.core :as reabledit]
            [reabledit.components :as components])
  (:require-macros [devcards.core :as dc :refer [defcard-rg]]))

(defn generate-row
  [id columns]
  (reduce
   (fn [row column]
     (assoc row (:key column) (str (gensym "Some really long, long prefix"))))
   {:id id} columns))

(def row-count 100)
(def column-count 30)

(def example-columns (mapv #(assoc {} :key (str %) :value (str "Column " %))
                           (range 1 column-count)))
(def example-data (mapv #(generate-row % example-columns) (range 1 row-count)))

(defonce example-atom (reagent/atom example-data))

(defn example-row-fn
  [nth-row old-row new-row]
  (swap! example-atom reabledit/update-row nth-row new-row))

(defcard-rg reabledit
  (fn [data-atom _]
    [:div
     {:style {:height "430px"}}
     [reabledit/data-table {:columns example-columns
                            :data @data-atom
                            :primary-key :id
                            :row-change-fn example-row-fn}]])
  example-atom)
