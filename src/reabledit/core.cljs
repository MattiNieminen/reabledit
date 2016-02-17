(ns reabledit.core
  (:require [reabledit.components :as components]
            [reabledit.keyboard :as keyboard]
            [reabledit.util :as util]
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
  (let [element-id (gensym "reabledit-focusable")
        state (reagent/atom {})]
    (fn [columns data row-change-fn]
      (let [enable-edit! (util/enable-edit-fn columns data state)
            disable-edit! (util/disable-edit-fn state row-change-fn element-id)
            set-selected! (util/set-selected-fn state disable-edit!)]
        [:div.reabledit
         {:id element-id
          :tabIndex 0
          :on-key-down #(keyboard/handle-key-down %
                                                  columns
                                                  data
                                                  state
                                                  enable-edit!
                                                  disable-edit!
                                                  set-selected!)
          :on-double-click enable-edit!}
         [components/data-table-headers columns]
         (for [[nth-row row-data] (map-indexed vector data)]
           ^{:key nth-row}
           [components/data-table-row
            columns
            data
            row-data
            nth-row
            state
            set-selected!])]))))
