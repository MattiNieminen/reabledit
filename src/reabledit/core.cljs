(ns reabledit.core
  (:require [reagent.core :as reagent]))

;;
;; State-related helpers
;;

(defn set-selected!
  [state row col]
  (swap! state assoc :selected [row col]))

(defn enable-edit!
  [data state]
  (let [row-data (nth data (first (:selected @state)))]
    (swap! state assoc :edit {:initial row-data
                              :updated row-data})))

;;
;; Keyboard event handlers
;;

(defn handle-editing-mode-key-down
  [e state row-change-fn id]
  (if (= 13 (.-keyCode e))
    (do
      (.preventDefault e)
      (row-change-fn (get-in @state [:edit :initial])
                     (get-in @state [:edit :updated]))
      (swap! state dissoc :edit)
      ;; Dirty as fudge, but what can you do with Reagent?
      (.focus (.getElementById js/document id)))
    nil))

(defn handle-selection-mode-key-down
  [e headers data state]
  (.preventDefault e)
  (let [keycode (.-keyCode e)
        current-row (-> @state :selected first)
        current-col (-> @state :selected second)
        rows (-> data count dec)
        cols (-> headers count dec)
        f (partial set-selected! state)]
    (condp = keycode
      37 (f current-row (max 0 (dec current-col)))
      38 (f (max 0 (dec current-row)) current-col)
      39 (f current-row (min cols (inc current-col)))
      40 (f (min rows (inc current-row)) current-col)
      9 (f current-row (min cols (inc current-col)))
      13 (enable-edit! data state)
      nil)))

(defn handle-key-down
  [e headers data state row-change-fn id]
  (if (:edit @state)
    (handle-editing-mode-key-down e state row-change-fn id)
    (handle-selection-mode-key-down e headers data state)))

;;
;; Components
;;

(defn data-table-cell-input
  [headers data nth-row nth-col state]
  (let [k (first (nth headers nth-col))]
    [:input {:type "text"
             :auto-focus true
             :value (get (get-in @state [:edit :updated]) k)
             :on-change #(swap! state
                                assoc-in
                                [:edit :updated k]
                                (-> % .-target .-value))}]))

(defn data-table-cell
  [headers data v nth-row nth-col state]
  (let [selected? (= (:selected @state) [nth-row nth-col])
        edit? (:edit @state)]
    [:td {:class (if selected? "selected")
          :on-click #(set-selected! state nth-row nth-col)}
     (if (and selected? edit?)
       [data-table-cell-input headers data nth-row nth-col state]
       [:span v])]))

(defn data-table-row
  [headers data row-data nth-row state]
  [:tr
   ;; TODO: run map-indexed to headers only once
   (for [[nth-col [k _]] (map-indexed vector headers)]
     ^{:key nth-col}
     [data-table-cell headers data (get row-data k) nth-row nth-col state])])

(defn data-table-headers
  [headers]
  [:thead
   [:tr
    (for [[k localized] headers]
      ^{:key k} [:th localized])]])

(defn data-table
  [headers data row-change-fn]
  (let [state (reagent/atom {})
        id (gensym "reabledit-focusable")]
    (fn [headers data]
      [:div.reabledit
       {:id id
        :tabIndex 0
        :on-key-down #(handle-key-down % headers data state row-change-fn id)}
       [:table
        [data-table-headers headers]
        [:tbody
         (for [[nth-row row-data] (map-indexed vector data)]
           ^{:key nth-row}
           [data-table-row headers data row-data nth-row state])]]])))
