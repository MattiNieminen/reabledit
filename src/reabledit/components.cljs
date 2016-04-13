(ns reabledit.components
  (:require [reabledit.util :as util]
            [reagent.core :as reagent]
            [reagent.ratom :refer-macros [reaction]]))

;;
;; Cell views
;;

(defn span-view
  []
  (fn [v _]
    [:span v]))

(defn dropdown-view
  [options]
  (fn [v _]
    [:span (-> (filter #(= (:key %) v) options)
               first
               :value)]))

;;
;; Cell editors
;;

(defn string-editor
  []
  (fn [v change-fn _]
    [:input {:type "text"
             :auto-focus true
             :on-focus util/move-cursor-to-end!
             :value v
             :on-change #(change-fn (-> % .-target .-value))}]))

(defn int-editor
  []
  (fn [v change-fn _]
    [:input {:type "text"
             :auto-focus true
             :on-focus util/move-cursor-to-end!
             :value v
             :on-change (fn [e]
                          (let [new-value (js/parseInt (-> e .-target .-value))
                                int? (not (js/isNaN new-value))]
                            (if int?
                              (change-fn new-value))))}]))

(defn- dropdown-editor-on-key-down
  [e v change-fn options]
  (let [keycode (.-keyCode e)
        position (first (keep-indexed #(if (= %2 v) %1)
                                      (map :key options)))]
    (case keycode
      38 (do
           (.preventDefault e)
           (if (zero? position)
             (change-fn (-> options last :key))
             (change-fn (:key (nth options (dec position))))))
      40 (do
           (.preventDefault e)
           (if (= position (-> options count dec))
             (change-fn (-> options first :key))
             (change-fn (:key (nth options (inc position))))))
      nil)))

(defn dropdown-editor
  [options]
  (fn [v change-fn disable-edit!]
    (reagent/create-class
     {:component-did-mount #(.focus (reagent/dom-node %))
      :reagent-render
      (fn [v change-fn disable-edit!]
        [:div.reabledit-dropdown
         {:tabIndex 0
          :on-key-down #(dropdown-editor-on-key-down % v change-fn options)}
         [:span (-> (filter #(= (:key %) v) options)
                    first
                    :value)]
         [:div.reabledit-dropdown-items
          (for [{:keys [key value]} options]
            ^{:key key}
            [:div.reabledit-dropdown-item
             {:class (if (= key v) "selected")
              :on-click (fn [e]
                          (.stopPropagation e)
                          (change-fn key)
                          (disable-edit!))}
             [:span value]])]])})))

;;
;; Dependencies for the main component
;;

(defn data-table-cell
  [column row-change-fn row-data nth-row nth-col rows cols state]
  (let [{:keys [selected? edited?]} @(reagent/track util/cell-info
                                                    nth-row
                                                    nth-col
                                                    state)
        enable-edit! #(util/enable-edit! column row-data state)
        move-to-cell! #(util/move-to-cell! row-change-fn
                                           nth-row
                                           nth-col
                                           state)]
    [:div.reabledit-cell
     {:id (util/cell-id nth-row nth-col)
      :class (if selected? "selected")
      :tabIndex 0
      :on-key-down #(util/default-handle-key-down %
                                                  row-change-fn
                                                  column
                                                  row-data
                                                  rows
                                                  cols
                                                  state)
      :on-click #(move-to-cell!)
      :on-double-click #(enable-edit!)}
     (if edited?
       (let [editor (or (:editor column) (string-editor))
             path [:edit :updated (:key column)]]
         [editor
          (get-in @state path)
          #(swap! state assoc-in path %)
          move-to-cell!])
       (let [view (or (:view column) (span-view))]
         [view (get row-data (:key column)) enable-edit!]))]))

(defn data-table-row
  [columns row-change-fn row-data nth-row rows cols state]
  [:div.reabledit-row
   (for [[nth-col column] (map-indexed vector columns)]
     ^{:key nth-col}
     [data-table-cell
      column
      row-change-fn
      row-data
      nth-row
      nth-col
      rows
      cols
      state])])

(defn data-table-headers
  [columns]
  [:div.reabledit-row.reabledit-column-row
   (for [{:keys [key value]} columns]
     ^{:key key}
     [:div.reabledit-cell.reabledit-header
      [:span value]])])
