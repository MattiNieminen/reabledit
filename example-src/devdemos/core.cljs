(ns devdemos.core
  (:require [reagent.core :as reagent]
            [reabledit.core :as reabledit]
            [reabledit.components :as components])
  (:require-macros [devcards.core :as dc :refer [defcard-rg]]))

(def example-columns
  [{:key :id
    :value "ID"
    :disable-edit true}
   {:key :name
    :value "Name"}
   {:key :age
    :value "Age"
    :editor (components/int-editor)}])

(def example-data
  [{:id 1
    :name "Deadpool"
    :age 32}
   {:id 2
    :name "Supergirl"
    :age 27}
   {:id 3
    :name "Dr. Strange"
    :age "43"}])

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
  example-atom
  {:inspect-data true})
