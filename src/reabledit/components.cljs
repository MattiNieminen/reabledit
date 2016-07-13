(ns reabledit.components
  (:require [reabledit.util :as util]
            [reagent.core :as reagent]
            [reagent.ratom :refer-macros [reaction]]
            [clojure.string :as str]))

;;
;; Cell views
;;

(defn view-template
  ([v]
   [view-template v util/default-copy util/default-paste util/default-cut])
  ([v copy! paste! cut!]
   [:div.reabledit-cell-view
    [:span.reabledit-cell__content v]
    [:input.reabledit-cell-view__hidden-input-handler.reabledit-focused
     {:on-copy copy!
      :on-paste paste!
      :on-cut cut!}]]))

(defn default-view
  [row-data k _ _]
  [view-template (get row-data k)])

(defn dropdown-view
  [row-data k _ {:keys [options]}]
  [view-template (-> (filter #(= (:key %) (get row-data k)) options)
                     first
                     :value)])

;;
;; Cell editors
;;

(defn default-editor
  [row-data k change-fn _]
  [:input.reabledit-cell-editor-input
   {:type "text"
    :auto-focus true
    :on-focus util/move-cursor-to-end!
    :value (get row-data k)
    :on-change #(change-fn (assoc row-data
                                  k
                                  (-> % .-target .-value)))}])

(defn int-coercable?
  [s]
  (re-matches #"\s*((0[,.]0*)|0|([1-9][0-9]*)([.,]0*)?)\s*" s))

(defn int-editor
  [row-data k change-fn _]
  (let [initial-value (get row-data k)
        input (reagent/atom (str initial-value))]
    (fn [row-data k change-fn _]
      [:input.reabledit-cell-editor-input
       {:type "text"
        :class (if-not (int-coercable? @input)
                 "reabledit-cell-editor-input--error")
        :auto-focus true
        :on-focus util/move-cursor-to-end!
        :value @input
        :on-change (fn [e]
                     (let [new-input (-> e .-target .-value)
                           new-value (if (int-coercable? new-input)
                                       (js/parseInt new-input)
                                       initial-value)]
                       (reset! input new-input)
                       (change-fn (assoc row-data
                                         k
                                         new-value))))}])))

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
  [row-data k change-fn disable-edit! {:keys [options]}]
  (reagent/create-class
   {:component-did-mount #(.focus (reagent/dom-node %))
    :reagent-render
    (fn [row-data k change-fn disable-edit!]
      (let [v (get row-data k)
            change-fn #(change-fn (assoc row-data k %))]
        [:div.reabledit-cell-editor-dropdown
         {:tabIndex 0
          :on-key-down #(dropdown-editor-key-down % v change-fn options)}
         [:span.reabledit-cell__content
          (-> (filter #(= (:key %) v) options)
              first
              :value)]
         [:div.reabledit-cell-editor-dropdown-list
          (for [{:keys [key value]} options]
            ^{:key key}
            [:div.reabledit-cell-editor-dropdown-list__item
             {:class (if (= key v)
                       "reabledit-cell-editor-dropdown-list__item--selected")
              :on-click (fn [e]
                          (.stopPropagation e)
                          (change-fn key)
                          (disable-edit!))}
             [:span.reabledit-cell__content value]])]]))}))

;;
;; Dependencies for the main component
;;

(defn data-table-cell
  [primary-key row-change-fn state column-keys row-ids row-data column]
  (let [column-key (:key column)
        row-id (get row-data primary-key)
        {:keys [selected? edited? width]} @(reagent/track util/cell-info
                                                          state
                                                          column-key
                                                          row-id)
        enable-edit! #(util/enable-edit! state row-data column)
        move-to-cell! #(util/move-to-cell! row-change-fn
                                           state
                                           row-ids
                                           column-key
                                           row-id)]
    [:div.reabledit-cell
     {:id (util/cell-id column-key row-id)
      :class (if selected? "reabledit-cell--selected")
      :tabIndex 0
      :style {:width (util/column-width (count column-keys) width)}
      :on-key-down #(util/default-handle-key-down %
                                                  row-change-fn
                                                  state
                                                  column-keys
                                                  row-ids
                                                  row-data
                                                  column
                                                  column-key
                                                  row-id)
      :on-click #(if-not edited?
                   (move-to-cell!))
      :on-double-click #(enable-edit!)}
     (if edited?
       [(or (:editor column) default-editor)
        (get-in @state [:edit :updated])
        column-key
        #(swap! state assoc-in [:edit :updated] %)
        move-to-cell!
        (:opts column)]
       [(or (:view column) default-view)
        row-data
        column-key
        enable-edit!
        (:opts column)])]))

(defn data-table-row
  [columns primary-key row-change-fn state column-keys row-ids row-data]
  [:div.reabledit-row
   (for [column columns]
     ^{:key (:key column)}
     [data-table-cell
      primary-key
      row-change-fn
      state
      column-keys
      row-ids
      row-data
      column])])

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
    [:div.reabledit-row.reabledit-row--header
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
       [:div.reabledit-cell.reabledit-cell--header
        {:id (util/header-id key)
         :style {:width (util/column-width (count columns)
                                           (get-in column-data [key :width]))}}
        [:span.reabledit-cell__content.reabledit-cell__content--header value]
        [:div.reabledit-cell__header-handle
         {:draggable true
          :on-drag-start #(start-resize! % key state)
          :on-drag-end #(stop-resize! state)}]])
     (if (> scrollbar-size 0)
       [:div.reabledit-cell__header-scroll
        {:style {:min-width (str scrollbar-size "px")}}])]))
