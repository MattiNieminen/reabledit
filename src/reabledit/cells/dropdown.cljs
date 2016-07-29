(ns reabledit.cells.dropdown
  (:require [reabledit.util :as util]
            [clojure.string :as str]
            [reagent.core :as reagent]))

(defn set-state!
  [state v]
  (reset! state {:edit? true
                 :selected v}))

(defn handle-key-down
  [e state options k commit!]
  (let [keycode (.-keyCode e)
        {:keys [edit? selected]} @state
        position (util/find-index (map :key options) selected)
        set-selected! (fn [k]
                        (.preventDefault e)
                        (.stopPropagation e)
                        (set-state! state k))]
    (cond

      ;; Enter and F2 start edit mode with old value
      (and (not edit?) (or (= keycode 13) (= keycode 113)))
      (set-selected! k)

      ;; Arrow keys navigate the dropdown up and down
      (and edit? (= keycode 38))
      (if (zero? position)
        (set-selected! (-> options last :key))
        (set-selected! (:key (nth options (dec position)))))

      (and edit? (= keycode 40))
      (if (= position (-> options count dec))
        (set-selected! (-> options first :key))
        (set-selected! (:key (nth options (inc position)))))

      ;; Enter commits the changes when dropdown is edit
      (and edit? (= keycode 13))
      (commit!)

      ;; Navigation with arrow keys is blocked when dropdown is edit
      (and edit? (contains? #{37 39} keycode))
      (.stopPropagation e)

      :else nil)))

(defn handle-on-change
  [e state options k]
  (let [input (str/lower-case (-> e .-target .-value))
        option (first (filter #(str/starts-with? (-> % :value str/lower-case)
                                                 input)
                              options))]
    (set-state! state (or (:key option) k))))

(defn handle-paste
  [e state options commit!]
  (let [input (str/lower-case (util/get-clipboard-data e))
        option (first (filter #(= (-> % :value str/lower-case)
                                  input)
                              options))]
    (when option
      (set-state! state (:key option))
      (commit!))))

(defn dropdown-cell
  [{:keys [row-data column-key commit! opts]}]
  (let [state (reagent/atom nil)]
    (fn [{:keys [row-data column-key commit! opts]}]
      (let [options (:options opts)
            k (get row-data column-key)
            v (-> (filter #(= (:key %) k) options) first :value)
            commit! (fn []
                      (if (and (:edit? @state) (not= (:selected @state) k))
                        (commit! (assoc row-data
                                        column-key
                                        (:selected @state))))
                      (reset! state nil))
            toggle-options! #(if (:edit? @state)
                               (reset! state nil)
                               (set-state! state k))]
        [:div.reabledit-dropdown-cell
         {:on-double-click toggle-options!
          :title v}
         [:input.reabledit-dropdown-cell__input.reabledit-focused
          {:type "text"
           :value ""
           :on-key-down #(handle-key-down % state options k commit!)
           :on-change #(handle-on-change % state options k)

           ;; A hack. Should use relatedTarget, but Firefox
           ;; does not support it yet. Fix in the future.
           :on-blur #(if (:edit? @state) (js/setTimeout commit! 500))
           :on-copy #(util/set-clipboard-data % v)
           :on-paste #(handle-paste % state options commit!)
           :on-cut #(util/set-clipboard-data % v)}]
         [:div.reabledit-dropdown-cell-view
          [:span.reabledit-dropdown-cell-view__text v]
          [:span.reabledit-dropdown-cell-view__caret
           {:on-click toggle-options!}
           "▼"]]
         (if (:edit? @state)
           (let [selected-key (:selected @state)]
             [:div.reabledit-dropdown-cell-options
              (for [{:keys [key value]} options]
                ^{:key key}
                [:div.reabledit-dropdown-cell-options__item
                 {:class (if (= selected-key key)
                           "reabledit-dropdown-cell-options__item--selected")
                  :on-click (fn [e]
                              (set-state! state key)
                              (commit!))}
                 value])]))]))))
