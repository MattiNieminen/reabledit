(ns reabledit.core
  (:require [reagent.core :as reagent]))

(defn data-table-cell
  [v]
  [:td v])

(defn data-table-row
  [headers data]
  [:tr
   (for [[k _] headers]
     ^{:key k} [data-table-cell (get data k)])])

(defn data-table-headers
  [headers]
  [:thead
   [:tr
    (for [[k localized] headers]
      ^{:key k} [:th localized])]])

(defn data-table
  [headers data unique-key]
  [:div.reabledit
   [:table
    [data-table-headers headers]
    [:tbody
     (for [row data]
       ^{:key (get row unique-key)} [data-table-row headers row unique-key])]]])
