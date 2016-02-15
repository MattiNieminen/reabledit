(ns reabledit.components
  (:require [reabledit.util :as util]))

(defn data-table-cell-input
  [columns data nth-row nth-col state]
  (let [k (:key (nth columns nth-col))]
    [:input {:type "text"
             :auto-focus true
             :value (get (get-in @state [:edit :updated]) k)
             :on-change #(swap! state
                                assoc-in
                                [:edit :updated k]
                                (-> % .-target .-value))}]))

(defn data-table-cell
  [columns data v nth-row nth-col state]
  (let [selected? (= (:selected @state) [nth-row nth-col])
        edit? (:edit @state)]
    [:td {:class (if selected? "selected")
          :on-click #(util/set-selected! state nth-row nth-col)}
     (if (and selected? edit?)
       [data-table-cell-input columns data nth-row nth-col state]
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
