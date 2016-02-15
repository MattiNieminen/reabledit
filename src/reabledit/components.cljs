(ns reabledit.components
  (:require [reabledit.util :as util]
            [reagent.core :as reagent]))

;;
;; Cell editors
;;

(defn string-editor
  [cursor]
  [:input {:type "text"
           :auto-focus true
           :value @cursor
           :on-change #(reset! cursor (-> % .-target .-value))}])

(defn int-editor
  [cursor]
  [:input {:type "text"
           :auto-focus true
           :value @cursor
           :on-change (fn [e]
                        (let [new-value (js/parseInt (-> e .-target .-value))
                              int? (not (js/isNaN new-value))]
                          (if int?
                            (reset! cursor new-value))))}])

;;
;; Dependencies for the main component
;;

(defn data-table-cell
  [columns data v nth-row nth-col state]
  (let [selected? (= (:selected @state) [nth-row nth-col])
        edit? (:edit @state)
        column (nth columns nth-col)
        cursor (reagent/cursor state [:edit :updated (:key column)])
        editor (or (:editor column) string-editor)]
    [:td {:class (if selected? "selected")
          :on-click #(util/set-selected! state nth-row nth-col)}
     (if (and selected? edit?)
       [editor cursor]
       [:span v])]))

(defn data-table-row
  [columns data row-data nth-row state]
  [:tr
   ;; TODO: run map-indexed to columns only once
   (for [[nth-col {:keys [key value]}] (map-indexed vector columns)]
     ^{:key nth-col}
     [data-table-cell columns data (get row-data key) nth-row nth-col state])])

(defn data-table-headers
  [columns]
  [:thead
   [:tr
    (for [{:keys [key value]} columns]
      ^{:key key} [:th value])]])
