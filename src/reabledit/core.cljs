(ns reabledit.core
  (:require [reabledit.components :as components]
            [reabledit.util :as util]
            [reagent.core :as reagent]))

;;
;; Convenience API
;;

(defn update-row
  [data nth-row new-row]
  (assoc data nth-row new-row))

;;
;; Main component
;;

(defn data-table
  [columns data row-change-fn]
  (let [state (reagent/atom {})]
    (fn [columns data row-change-fn]
      (let [rows (-> data count dec)
            cols (-> columns count dec)]
        [:div.reabledit
         [components/data-table-headers columns]
         (for [[nth-row row-data] (map-indexed vector data)]
           ^{:key nth-row}
           [components/data-table-row
            columns
            row-change-fn
            row-data
            nth-row
            rows
            cols
            state])]))))
