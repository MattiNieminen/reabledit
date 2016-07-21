(ns devdemos.lots-of-data
  (:require [reagent.core :as reagent]
            [reabledit.core :as reabledit]
            [reabledit.cells :as cells])
  (:require-macros [devcards.core :as dc :refer [defcard-rg]]))

(defn generate-row
  [id columns]
  (reduce
   (fn [row column]
     (assoc row (:key column) (str (gensym "Some really long, long prefix"))))
   {:id id} columns))

(def row-count 100)
(def column-count 30)

(def example-columns (mapv (fn [n]
                             {:key (str n)
                              :value (str "Column " n)
                              :cell cells/standard-cell})
                           (range 1 column-count)))
(def example-data (mapv #(generate-row % example-columns) (range 1 row-count)))

(defonce example-atom (reagent/atom example-data))

(defn example-row-change-fn
  [nth-row new-row]
  (swap! example-atom reabledit/update-row nth-row new-row))

(defcard-rg reabledit
  (fn [data-atom _]
    [:div
     {:style {:height "430px"}}
     [reabledit/data-table {:columns example-columns
                            :data @data-atom
                            :primary-key :id
                            :row-change-fn example-row-change-fn}]])
  example-atom)
