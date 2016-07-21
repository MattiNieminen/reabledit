(ns reabledit.cells.standard
  (:require [reabledit.util :as util]
            [reagent.core :as reagent]))

;; TODO: the following functions will help when clojure.spec
;; is out.

(defn int-coercable?
  [s]
  (re-matches #"\s*((0[,.]0*)|0|([1-9][0-9]*)([.,]0*)?)\s*" s))

(defn parse-int
  [s fallback]
  (if (int-coercable? s)
    (js/parseInt s)
    fallback))

(defn handle-key-down
  [e input v commit!]
  (let [keycode (.-keyCode e)]
    (cond

      ;; Enter and F2 start edit mode from clean state
      (and (not @input)
           (contains? #{13 113} keycode))
      (do
        (.preventDefault e)
        (.stopPropagation e)
        (reset! input v))

      ;; Enter in edit mode commits changes
      (and @input (= keycode 13))
      (do
        (.preventDefault e)
        (commit!))

      ;; Navigation with arrow keys is blocked in
      ;; edit mode
      (and @input (contains? #{37 38 39 40} keycode))
      (.stopPropagation e)

      :else nil)))

;; TODO: use clojure.spec and opts to provide conformation to ints and doubles

(defn standard-cell
  [{:keys [row-data column-key commit!]}]
  (let [input (reagent/atom nil)]
    (fn [{:keys [row-data column-key commit!]}]
      (let [v (get row-data column-key)
            commit! (fn []
                      (if (and @input (not= @input v))
                        (commit! (assoc row-data column-key @input)))
                      (reset! input nil))]
        [:div.reabledit-standard-cell
         {:on-double-click #(reset! input v)
          :title v}
         [:input.reabledit-standard-cell__item.reabledit-focused
          {:class (if-not @input "reabledit-standard-cell__item--hidden")
           :type "text"
           :value @input
           :on-change #(reset! input (-> % .-target .-value))
           :on-key-down #(handle-key-down % input v commit!)
           :on-focus util/move-cursor-to-end!
           :on-blur commit!
           :on-copy #(if-not @input (util/set-clipboard-data % v))
           :on-paste #(when (not @input)
                        (reset! input (util/get-clipboard-data %))
                        (commit!))
           :on-cut #(when (not @input)
                      (util/set-clipboard-data % v)
                      (reset! input "")
                      (commit!))}]
         (if-not @input
           [:span.reabledit-standard-cell__item
            v])]))))
