(ns reabledit.core
  (:require [reagent.core :as reagent]))

;;
;; State-related helpers
;;

(defn set-selected!
  [ui-state row col]
  (swap! ui-state assoc :selected [row col]))

(defn set-edited!
  [ui-state row col]
  (swap! ui-state assoc :edited [row col]))

;;
;; Keyboard event handlers
;;

(defn handle-editing-mode-key-down
  [e ui-state id]
  (if (= 13 (.-keyCode e))
    (do (swap! ui-state dissoc :edited)
        ;; Dirty as fudge, but what can you do with Reagent?
        (.focus (.getElementById js/document id)))
    nil))

(defn handle-selection-mode-key-down
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
      9 (f current-row (min (dec cols) (inc current-col)))
      13 (set-edited! ui-state current-row current-col)
      nil)))

(defn handle-key-down
  [e rows cols ui-state id]
  (.preventDefault e)
  (if (:edited @ui-state)
    (handle-editing-mode-key-down e ui-state id)
    (handle-selection-mode-key-down e rows cols ui-state)))

;;
;; Components
;;

(defn data-table-cell
  [v nth-row nth-col ui-state]
  (let [selected? (= (:selected @ui-state) [nth-row nth-col])
        edited? (= (:edited @ui-state) [nth-row nth-col])]
    [:td {:class (if selected? "selected")
          :on-click #(set-selected! ui-state nth-row nth-col)}
     (if edited?
       [:input {:type "text"
                :auto-focus true
                :value v}]
       [:span v])]))

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
  (let [ui-state (reagent/atom {})
        id (gensym "reabledit-focusable")]
    (fn [headers data]
      [:div.reabledit
       {:id id
        :tabIndex 0
        :on-key-down #(handle-key-down %
                                       (count data)
                                       (count headers)
                                       ui-state
                                       id)}
       [:table
        [data-table-headers headers]
        [:tbody
         (for [[nth-row row-data] (map-indexed vector data)]
           ^{:key nth-row}
           [data-table-row headers row-data nth-row ui-state])]]])))
