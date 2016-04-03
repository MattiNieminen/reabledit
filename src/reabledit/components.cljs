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
  (with-meta
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
           [:span value]])]])
    {:component-did-mount
     (fn [this]
       (.focus (reagent/dom-node this)))}))

;;
;; Dependencies for the main component
;;

(defn data-table-cell
  [columns v nth-row nth-col state
   enable-edit! disable-edit! set-selected!]
  (let [selected? (reaction (= (:selected @state) [nth-row nth-col]))
        edit? (reaction (:edit @state))]
    (fn [columns v nth-row nth-col state
         enable-edit! disable-edit! set-selected!]
      (let [column (nth columns nth-col)
            view (or (:view column) (span-view))
            editor (or (:editor column) (string-editor))]
        [:div.reabledit-cell {:class (if @selected? "selected")
                              :on-click #(set-selected! nth-row nth-col)
                              :on-double-click enable-edit!}
         (if (and @selected? @edit?)
           (let [path [:edit :updated (:key column)]
                 value-in-state (get-in @state path)
                 change-fn #(swap! state assoc-in path %)]
             [editor value-in-state change-fn disable-edit!])
           [view v enable-edit!])]))))

(defn data-table-row
  [columns row-data nth-row state
   enable-edit! disable-edit! set-selected!]
  [:div.reabledit-row
   ;; TODO: run map-indexed to columns only once
   (for [[nth-col {:keys [key value]}] (map-indexed vector columns)]
     ^{:key nth-col}
     [data-table-cell
      columns
      (get row-data key)
      nth-row
      nth-col
      state
      enable-edit!
      disable-edit!
      set-selected!])])

(defn data-table-headers
  [columns]
  [:div.reabledit-row
   (for [{:keys [key value]} columns]
     ^{:key key}
     [:div.reabledit-cell.reabledit-header
      [:span value]])])
