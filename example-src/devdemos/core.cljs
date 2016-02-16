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
    :editor components/int-editor}
   {:key :owner
    :value "Owner"}])

(def example-data
  [{:id 1
    :name "Rin Tin Tin"
    :age 3
    :owner "Lee Duncan"}
   {:id 2
    :name "Lassie"
    :age 10
    :owner "Jeff"}
   {:id 3
    :name "Krypto"
    :age "24"
    :owner "Clark Kent"}])

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
