(ns reabledit.components
  (:require [reabledit.util :as util]
            [reagent.core :as reagent]
            [goog.dom.classlist :as classlist]))

(defn data-table-cell
  [{:keys [primary-key row-change-fn state
           column-keys row-ids row-data column]}]
  (reagent/create-class
   {:component-did-update
    (fn [this _]
      (if (classlist/contains (reagent/dom-node this)
                              "reabledit-cell--selected")
        (util/fix-focus! (reagent/dom-node this))))
    :reagent-render
    (fn [{:keys [primary-key row-change-fn state
                 column-keys row-ids row-data column]}]
      (let [column-key (:key column)
            row-id (get row-data primary-key)
            {:keys [selected? width]} @(reagent/track util/cell-state
                                                      state
                                                      column-key
                                                      row-id)]
        [:div.reabledit-cell
         {:id (str "reabledit-cell-" row-id "-" column-key)
          :class (if selected? "reabledit-cell--selected")
          :style {:width (util/column-width (count column-keys) width)}
          :on-click (fn [e]
                      (util/move-to-cell! state row-id column-key)
                      (util/fix-focus! (-> e .-currentTarget)))
          :on-double-click #(util/fix-focus! (-> % .-currentTarget))}
         [(:cell column)
          {:row-data row-data
           :column-key column-key
           :selected? selected?
           :commit! #(row-change-fn (util/find-index row-ids row-id) %)
           :opts (:opts column)}]]))}))

(defn data-table-row
  [{:keys [columns primary-key row-change-fn state
           column-keys row-ids row-data]}]
  [:div.reabledit-row
   (for [column columns]
     ^{:key (:key column)}
     [data-table-cell {:primary-key primary-key
                       :row-change-fn row-change-fn
                       :state state
                       :column-keys column-keys
                       :row-ids row-ids
                       :row-data row-data
                       :column column}])])

(defn data-table-headers
  [{:keys [columns state]}]
  (let [column-widths (:column-widths @state)]
    [:div.reabledit-row.reabledit-row--header
     (if (:resize @state)
       [:div.reabledit-resize-area
        {:on-drag-over (fn [e]
                         (.preventDefault e)
                         (util/resize! e state))
         :on-drop (fn [e]
                    (.preventDefault e)
                    (util/stop-resize! state))}])
     (for [{:keys [key value]} columns]
       ^{:key key}
       [:div.reabledit-cell.reabledit-cell--header
        {:id (util/header-id key)
         :style {:width (util/column-width (count columns)
                                           (get column-widths key))}}
        [:span.reabledit-cell__header-text value]
        [:div.reabledit-cell__header-handle
         {:draggable true
          :on-drag-start #(util/start-resize! % key state)
          :on-drag-end #(util/stop-resize! state)}]])
     [:div.reabledit-cell__header-scroll]]))
