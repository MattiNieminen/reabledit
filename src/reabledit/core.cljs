(ns reabledit.core
  (:require [reagent.core :as reagent]))

;;
;; State-related helpers
;;

(defn set-selected!
  [ui-state row col]
  (swap! ui-state assoc :selected [row col]))


;;
;; Keyboard event handlers
;;

(defn handle-key-down
  [e rows cols ui-state]
  (let [keycode (.-keyCode e)
        current-row (first (:selected @ui-state))
        current-col (second (:selected @ui-state))
        f (partial set-selected! ui-state)]
    (condp = keycode
      37 (f current-row (max 0 (dec current-col)))
      38 (f (max 0 (dec current-row)) current-col)
      39 (f current-row (min (dec cols) (inc current-col)))
      40 (f (min (dec rows) (inc current-row)) current-col)
      nil)))

;;
;; Components
;;

(defn data-table-cell
  [v nth-row nth-col ui-state]
  [:td {:class (if (= (:selected @ui-state) [nth-row nth-col]) "selected")
        :on-click #(set-selected! ui-state nth-row nth-col)} v])

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
       {:tabIndex 0
        :on-key-down #(handle-key-down % (count data) (count headers) ui-state)}
       [:table
        [data-table-headers headers]
        [:tbody
         (for [[nth-row row-data] (map-indexed vector data)]
           ^{:key nth-row}
           [data-table-row headers row-data nth-row ui-state])]]])))
