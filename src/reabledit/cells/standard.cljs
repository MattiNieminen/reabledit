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

(defn set-state!
  [state v]
  (reset! state {:edit? true
                 :input v}))

(defn handle-key-down
  [e state v commit!]
  (let [keycode (.-keyCode e)
        {:keys [edit? input]} @state]
    (cond

      ;; Enter and F2 start edit mode with old value
      (and (not edit?) (contains? #{13 113} keycode))
      (do
        (.preventDefault e)
        (.stopPropagation e)
        (set-state! state v))

      ;; Enter in edit mode commits changes
      (and edit? (= keycode 13))
      (do
        (.preventDefault e)
        (commit!))

      ;; Navigation with arrow keys is blocked in
      ;; edit mode
      (and edit? (contains? #{37 38 39 40} keycode))
      (.stopPropagation e)

      :else nil)))

;; TODO: use clojure.spec and opts to provide conformation to ints and doubles

(defn standard-cell
  [{:keys [row-data column-key commit!]}]
  (let [state (reagent/atom nil)]
    (fn [{:keys [row-data column-key commit!]}]
      (let [v (get row-data column-key)
            commit! (fn []
                      (if (and (:edit? @state) (not= (:input @state) v))
                        (commit! (assoc row-data column-key (:input @state))))
                      (reset! state nil))]
        [:div.reabledit-standard-cell
         {:on-double-click #(if-not (:edit? @state) (set-state! state v))
          :title v}
         [:input.reabledit-standard-cell__input.reabledit-focused
          {:class (if-not (:edit? @state)
                    "reabledit-standard-cell__input--hidden")
           :type "text"
           :value (:input @state)
           :on-change #(set-state! state (-> % .-target .-value))
           :on-key-down #(handle-key-down % state v commit!)
           :on-focus util/move-cursor-to-end!
           :on-blur commit!
           :on-copy #(if-not (:edit? @state) (util/set-clipboard-data % v))
           :on-paste #(when (not (:edit? @state))
                        (set-state! state (util/get-clipboard-data %))
                        (commit!))
           :on-cut #(when (not (:edit? @state))
                      (util/set-clipboard-data % v)
                      (set-state! state "")
                      (commit!))}]
         (if-not (:edit? @state)
           [:span.reabledit-standard-cell__view
            v])]))))
