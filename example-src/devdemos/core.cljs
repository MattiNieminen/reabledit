(ns devdemos.core
  (:require [reagent.core :as reagent]
            [reabledit.core :as reabledit]
            [reabledit.components :as components])
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
    :disable-edit true}
   {:key :name
    :value "Name (not the secret identity)"}
   {:key :age
    :value "Age"
    :editor components/int-editor}
   {:key :publisher
    :value "Publisher"
    :view components/dropdown-view
    :editor components/dropdown-editor
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

(defn example-row-fn
  [nth-row old-row new-row]
  (swap! example-atom reabledit/update-row nth-row new-row))

(defcard-rg reabledit
  (fn [data-atom _]
    [:div
     [:button
      {:on-click (fn [e]
                   (let [new-row-id (inc (apply max (map :id @data-atom)))]
                     (swap! data-atom #(vec (cons {:id new-row-id} %)))))}
      "Add row to top"]
     [reabledit/data-table example-columns @data-atom :id example-row-fn]])
  example-atom
  {:inspect-data true})
