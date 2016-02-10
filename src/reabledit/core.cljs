(ns reabledit.core
  (:require [reagent.core :as reagent]))

;;
;; State-related helpers
;;

(defn set-selected!
  [ui-state row col]
  (swap! ui-state assoc :selected [row col]))

(defn enable-edit-mode!
  [ui-state row col]
  (swap! ui-state assoc :edit-mode true))

;;
;; Keyboard event handlers
;;

(defn handle-editing-mode-key-down
  [e ui-state id]
  (if (= 13 (.-keyCode e))
    (do
      (.preventDefault e)
      (swap! ui-state dissoc :edit-mode)
      ;; Dirty as fudge, but what can you do with Reagent?
      (.focus (.getElementById js/document id)))
    nil))

(defn handle-selection-mode-key-down
  [e rows cols ui-state]
  (.preventDefault e)
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
      13 (enable-edit-mode! ui-state current-row current-col)
      nil)))

(defn handle-key-down
  [e rows cols ui-state id]
  (if (:edit-mode @ui-state)
    (handle-editing-mode-key-down e ui-state id)
    (handle-selection-mode-key-down e rows cols ui-state)))

;;
;; Components
;;

(defn data-table-cell
  [v nth-row nth-col ui-state]
  (let [selected? (= (:selected @ui-state) [nth-row nth-col])
        edit-mode? (:edit-mode @ui-state)]
    [:td {:class (if selected? "selected")
          :on-click #(set-selected! ui-state nth-row nth-col)}
     (if (and selected? edit-mode?)
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
