(ns devdemos.core
  (:require [reagent.core :as reagent]
            [reabledit.core :as reabledit])
  (:require-macros [devcards.core :as dc :refer [defcard-rg]]))

(def example-headers
  [[:name "Name"] [:age "Age"] [:owner "Owner"]])

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

(defn example-row-fn
  [old-row new-row]
  (js/alert (str "Row "
                 (pr-str old-row)
                 " was changed into "
                 (pr-str new-row))))

(defcard-rg reabledit
  (fn [data-atom _]
    [reabledit/data-table example-headers @data-atom example-row-fn])
  (reagent/atom example-data))
