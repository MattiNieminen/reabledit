(ns reabledit.core
  (:require [reagent.core :as reagent]))

(defn data-table-cell
  [v nth-row nth-col ui-state]
  [:td {:class (if (= (:selected @ui-state) [nth-row nth-col]) "selected")
        :on-click #(swap! ui-state assoc :selected [nth-row nth-col])} v])

(defn data-table-row
  [headers row-data nth-row ui-state]
  [:tr
   ;; TODO: run map-indexed to headers only once
   (for [[nth-col [k _]] (map-indexed vector headers)]
     ^{:key nth-col}
     [data-table-cell (get row-data k) nth-row nth-col ui-state])])

(defn data-table-headers
  [headers]
  [:thead
   [:tr
    (for [[k localized] headers]
      ^{:key k} [:th localized])]])

(defn data-table
  [headers data]
  (let [ui-state (reagent/atom {})]
    (fn [headers data]
      [:div.reabledit
       [:table
        [data-table-headers headers]
        [:tbody
         (for [[nth-row row-data] (map-indexed vector data)]
           ^{:key nth-row}
           [data-table-row headers row-data nth-row ui-state])]]])))
