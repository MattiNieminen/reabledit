(ns reabledit.cells.read-only
  (:require [reabledit.util :as util]))

(defn read-only-cell
  [{:keys [row-data column-key]}]
  (let [v (get row-data column-key)]
    [:div.reabledit-read-only-cell
     {:title v}
     [:input.reabledit-read-only-cell__input.reabledit-focused
      {:type "text"
       :value ""
       :on-key-down #(if (= (.-keyCode %) 13) (.stopPropagation %))
       :on-copy #(util/set-clipboard-data % v)}]
     [:span.reabledit-read-only-cell__view v]]))
