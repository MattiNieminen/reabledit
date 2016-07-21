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
  [{:keys [columns data primary-key row-change-fn]}]
  (let [state (reagent/atom {})]
    (reagent/create-class
     {:component-did-mount
      (fn [this]
        (swap! state assoc :main-el (reagent/dom-node this)))
      :reagent-render
      (fn [{:keys [columns data primary-key row-change-fn]}]
        (let [header-row-el (aget (dom/getElementsByClass
                                   "reabledit-row--header"
                                   (:main-el @state))
                                  0)
              column-keys (mapv :key columns)
              row-ids (mapv #(get % primary-key) data)]
          [:div.reabledit
           {:on-key-down #(util/handle-key-down % state column-keys row-ids)}
           [components/data-table-headers {:columns columns
                                           :state state}]
           [:div.reabledit-data-rows
            (if header-row-el
              {:style {:height (str "calc(100% - "
                                    (.-clientHeight header-row-el)
                                    "px)")}
               :on-scroll (fn [e]
                            (when (= (.-currentTarget e) (.-target e))
                              (set! (.-scrollLeft header-row-el)
                                    (-> e .-target .-scrollLeft))))})
            (for [row-data data]
              ^{:key (get row-data primary-key)}
              [components/data-table-row {:columns columns
                                          :primary-key primary-key
                                          :row-change-fn row-change-fn
                                          :state state
                                          :column-keys column-keys
                                          :row-ids row-ids
                                          :row-data row-data}])]]))})))
