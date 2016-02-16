(ns reabledit.core
  (:require [reabledit.components :as components]
            [reabledit.keyboard :as keyboard]
            [reagent.core :as reagent]))

;;
;; Convenience API
;;

(defn update-row
  [data nth-row new-row]
  (let [[init tail] (split-at nth-row data)]
    (into [] (concat init [new-row] (rest tail)))))


;;
;; Main component
;;

(defn data-table
  [columns data row-change-fn]
  (let [state (reagent/atom {})
        element-id (gensym "reabledit-focusable")]
    (fn [columns data row-change-fn]
      [:div.reabledit
       {:id element-id
        :tabIndex 0
        :on-key-down #(keyboard/handle-key-down %
                                                columns
                                                data
                                                state
                                                row-change-fn
                                                element-id)}
       [:table
        [components/data-table-headers columns]
        [:tbody
         (for [[nth-row row-data] (map-indexed vector data)]
           ^{:key nth-row}
           [components/data-table-row columns data row-data nth-row state])]]])))
