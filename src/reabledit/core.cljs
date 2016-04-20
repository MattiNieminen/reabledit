(ns reabledit.core
  (:require [reabledit.components :as components]
            [reabledit.util :as util]
            [reagent.core :as reagent]
            [goog.dom :as dom]))

;;
;; Convenience API
;;

(defn update-row
  [data nth-row new-row]
  (assoc data nth-row new-row))

;;
;; Main component
;;

(defn data-table
  [columns data row-change-fn]
  (let [state (reagent/atom {})]
    (reagent/create-class
     {:component-did-mount
      (fn [this]
        (swap! state assoc :main-el (reagent/dom-node this)))
      :reagent-render
      (fn [columns data row-change-fn]
        (let [rows (count data)
              cols (count columns)
              header-row-el (aget (dom/getElementsByClass
                                   "reabledit-header-row"
                                   (:main-el @state))
                                  0)]
          [:div.reabledit
           [components/data-table-headers columns state]
           [:div.reabledit-data-rows-container
            (if header-row-el
              {:style {:height (str "calc(100% - "
                                    (.-clientHeight header-row-el)
                                    "px)")}
               :on-scroll (fn [e]
                            (when (= (.-currentTarget e) (.-target e))
                              (set! (.-scrollLeft header-row-el)
                                    (-> e .-target .-scrollLeft))))})
            (for [[nth-row row-data] (map-indexed vector data)]
              ^{:key nth-row}
              [components/data-table-row
               columns
               row-change-fn
               row-data
               nth-row
               rows
               cols
               state])]]))})))
