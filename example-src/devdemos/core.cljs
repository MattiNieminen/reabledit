(ns devdemos.core
  (:require [reagent.core :as reagent]
            [reabledit.core :as reabledit]
            [reabledit.cells :as cells])
  (:require-macros [devcards.core :as dc :refer [defcard-rg]]))

(def example-options
  [{:key :unknown
    :value "Unknown"}
   {:key :marvel
    :value "Marvel"}
   {:key :dc
    :value "DC Comics"}])

(def example-columns
  [{:key :id
    :value "ID"
    :cell cells/read-only-cell}
   {:key :name
    :value "Name (not the secret identity)"
    :cell cells/standard-cell}
   {:key :age
    :value "Age"
    :cell cells/standard-cell}
   {:key :publisher
    :value "Publisher"
    :cell cells/dropdown-cell
    :opts {:options example-options}}])

(def example-data
  [{:id 1
    :name "Deadpool"
    :age 32
    :publisher :marvel}
   {:id 2
    :name "Supergirl"
    :age 27
    :publisher :dc}
   {:id 3
    :name "Dr. Strange"
    :age "43"
    :publisher :marvel}])

(defonce example-atom (reagent/atom example-data))

(defn example-row-change-fn
  [nth-row new-row]
  (swap! example-atom reabledit/update-row nth-row new-row))

(defcard-rg reabledit
  (fn [data-atom _]
    [:div
     [:button
      {:on-click (fn [e]
                   (let [new-row-id (inc (apply max (map :id @data-atom)))]
                     (swap! data-atom #(vec (cons {:id new-row-id} %)))))}
      "Add row to top"]
     [reabledit/data-table {:columns example-columns
                            :data @data-atom
                            :primary-key :id
                            :row-change-fn example-row-change-fn}]])
  example-atom
  {:inspect-data true})
