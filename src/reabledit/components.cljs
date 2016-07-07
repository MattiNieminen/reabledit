(ns reabledit.components
  (:require [reabledit.util :as util]
            [reagent.core :as reagent]
            [reagent.ratom :refer-macros [reaction]]
            [clojure.string :as str]))

;;
;; Cell views
;;

(defn string-view
  []
  (fn [row-data k _]
    [:span (get row-data k)]))

(defn dropdown-view
  [options]
  (fn [row-data k _]
    [:span (-> (filter #(= (:key %) (get row-data k)) options)
               first
               :value)]))

;;
;; Cell editors
;;

(defn string-editor
  []
  (fn [row-data k change-fn _]
    [:input {:type "text"
             :auto-focus true
             :on-focus util/move-cursor-to-end!
             :value (get row-data k)
             :on-change #(change-fn (assoc row-data
                                           k
                                           (-> % .-target .-value)))}]))

(defn int-coercable?
  [s]
  (re-matches #"\s*((0[,.]0*)|0|([1-9][0-9]*)([.,]0*)?)\s*" s))

(defn int-editor
  []
  (fn [row-data k change-fn _]
    (let [initial-value (get row-data k)
          input (reagent/atom (str initial-value))]
      (fn [row-data k change-fn _]
        [:input {:type "text"
                 :auto-focus true
                 :on-focus util/move-cursor-to-end!
                 :value @input
                 :class (if-not (int-coercable? @input) "coercion-error")
                 :on-change (fn [e]
                              (let [new-input (-> e .-target .-value)
                                    new-value (if (int-coercable? new-input)
                                                (js/parseInt new-input)
                                                initial-value)]
                                (reset! input new-input)
                                (change-fn (assoc row-data
                                                  k
                                                  new-value))))}]))))

(defn- dropdown-editor-key-down
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
  (fn [row-data k change-fn disable-edit!]
    (reagent/create-class
     {:component-did-mount #(.focus (reagent/dom-node %))
      :reagent-render
      (fn [row-data k change-fn disable-edit!]
        (let [v (get row-data k)
              change-fn #(change-fn (assoc row-data k %))]
          [:div.reabledit-dropdown
           {:tabIndex 0
            :on-key-down #(dropdown-editor-key-down % v change-fn options)}
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
               [:span value]])]]))})))

(defonce default-editor (string-editor))

;;
;; Dependencies for the main component
;;

(defn data-table-cell
  [columns data primary-key row-change-fn row-data column state]
  (let [{:keys [selected? edited? width]} @(reagent/track util/cell-info
                                                          primary-key
                                                          row-data
                                                          column
                                                          state)
        enable-edit! #(util/enable-edit! row-data column state)
        move-to-cell! #(util/move-to-cell! data
                                           primary-key
                                           row-change-fn
                                           row-data
                                           column
                                           state)]
    [:div.reabledit-cell
     {:id (util/cell-id primary-key row-data column)
      :class (if selected? "selected")
      :tabIndex 0
      :style {:width (util/column-width columns width)}
      :on-key-down #(util/default-handle-key-down %
                                                  columns
                                                  data
                                                  primary-key
                                                  row-change-fn
                                                  row-data
                                                  column
                                                  state)
      :on-click #(move-to-cell!)
      :on-double-click #(enable-edit!)}
     (if edited?
       [(or (:editor column) default-editor)
        (get-in @state [:edit :updated])
        (:key column)
        #(swap! state assoc-in [:edit :updated] %)
        move-to-cell!]
       [(or (:view column) (string-view))
        row-data
        (:key column)
        enable-edit!])]))

(defn data-table-row
  [columns data primary-key row-change-fn row-data state]
  [:div.reabledit-row
   (for [column columns]
     ^{:key (:key column)}
     [data-table-cell
      columns
      data
      primary-key
      row-change-fn
      row-data
      column
      state])])

(defn start-resize!
  [e k state]
  (swap! state assoc :resize k)
  (.setData (.-dataTransfer e) "Text" (name k))
  (set! (-> e .-dataTransfer .-effectAllowed) "move"))

(defn stop-resize!
  [state]
  (swap! state dissoc :resize))

(defn resize!
  [e state]
  (let [k (:resize @state)
        element (.getElementById js/document (util/header-id k))
        width (- (.-pageX e) (.-left (.getBoundingClientRect element)))]
    (swap! state assoc-in [:columns k :width] width)))

(defn data-table-headers
  [columns state]
  (let [column-data (:columns @state)
        scrollbar-size (util/vertical-scrollbar-size (:main-el @state))]
    [:div.reabledit-row.reabledit-header-row
     (if (:resize @state)
       [:div.reabledit-resize-area
        {:on-drag-over (fn [e]
                         (.preventDefault e)
                         (resize! e state))
         :on-drop (fn [e]
                    (.preventDefault e)
                    (stop-resize! state))}])
     (for [{:keys [key value]} columns]
       ^{:key key}
       [:div.reabledit-cell.reabledit-header
        {:id (util/header-id key)
         :style {:width (util/column-width columns
                                           (get-in column-data [key :width]))}}
        [:span.reabledit-header-text value]
        [:div.reabledit-header-handle
         {:draggable true
          :on-drag-start #(start-resize! % key state)
          :on-drag-end #(stop-resize! state)}]])
     (if (> scrollbar-size 0)
       [:div.reabledit-header-scroll
        {:style {:min-width (str scrollbar-size "px")}}])]))
