(ns reabledit.cells.dropdown
  (:require [reabledit.util :as util]
            [clojure.string :as str]
            [reagent.core :as reagent]))

(defn handle-key-down
  [e selected options k commit!]
  (let [keycode (.-keyCode e)
        position (util/find-index (map :key options) @selected)
        set-selected! (fn [k]
                        (.preventDefault e)
                        (.stopPropagation e)
                        (reset! selected k))]
    (cond

      ;; Enter and F2 start edit mode from clean state
      (and (not @selected) (or (= keycode 13) (= keycode 113)))
      (set-selected! k)

      ;; Arrow keys navigate the dropdown up and down
      (and @selected (= keycode 38))
      (if (zero? position)
        (set-selected! (-> options last :key))
        (set-selected! (:key (nth options (dec position)))))

      (and @selected (= keycode 40))
      (if (= position (-> options count dec))
        (set-selected! (-> options first :key))
        (set-selected! (:key (nth options (inc position)))))

      ;; Enter commits the changes when dropdown is open
      (and @selected (= keycode 13))
      (commit!)

      ;; Navigation with arrow keys is blocked in
      ;; edit mode
      (and @selected (contains? #{37 38 39 40} keycode))
      (.stopPropagation e)

      :else nil)))

(defn handle-on-change
  [e selected options k]
  (let [input (str/lower-case (-> e .-target .-value))
        option (first (filter #(str/starts-with? (-> % :value str/lower-case)
                                                 input)
                              options))]
    (reset! selected (or (:key option) k))))

(defn handle-paste
  [e selected options commit!]
  (let [input (str/lower-case (util/get-clipboard-data e))
        option (first (filter #(= (-> % :value str/lower-case)
                                  input)
                              options))]
    (when option
      (reset! selected (:key option))
      (commit!))))

(defn dropdown-cell
  [{:keys [row-data column-key commit! opts]}]
  (let [selected (reagent/atom nil)]
    (fn [{:keys [row-data column-key commit! opts]}]
      (let [options (:options opts)
            k (get row-data column-key)
            v (-> (filter #(= (:key %) k) options) first :value)
            commit! (fn []
                      (if (and @selected (not= @selected k))
                        (commit! (assoc row-data column-key @selected)))
                      (reset! selected nil))]
        [:div.reabledit-dropdown-cell
         {:on-double-click #(if @selected
                              (reset! selected nil)
                              (reset! selected k))
          :title v}
         [:input.reabledit-dropdown-cell__input.reabledit-focused
          {:type "text"
           :value ""
           :on-key-down #(handle-key-down % selected options k commit!)
           :on-change #(handle-on-change % selected options k)
           :on-copy #(util/set-clipboard-data % v)
           :on-paste #(handle-paste % selected options commit!)
           :on-cut #(util/set-clipboard-data % v)}]
         [:div.reabledit-dropdown-cell-view
          [:span.reabledit-dropdown-cell-ciew__text v]
          [:span.reabledit-dropdown-cell-view__caret
           (if @selected "▼" "►")]]
         (if-let [selected-key @selected]
           [:div.reabledit-dropdown-cell-options
            (for [{:keys [key value]} options]
              ^{:key key}
              [:div.reabledit-dropdown-cell-options__item
               {:class (if (= selected-key key)
                         "reabledit-dropdown-cell-options__item--selected")
                :on-click (fn [e]
                            (reset! selected key)
                            (commit!))}
               value])])]))))
