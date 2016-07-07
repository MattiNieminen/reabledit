(ns reabledit.util
  (:require [goog.dom :as dom]))

(def min-column-width 15)

(defn header-id
  [k]
  (str "reabledit-header-" (name k)))

(defn cell-id
  [primary-key row-data column]
  (str "reabledit-cell-" (get row-data primary-key) "-" (:key column)))

(defn selected-cell [primary-key row-data column]
  {:row (get row-data primary-key)
   :column (:key column)})

(defn cell-info
  [primary-key row-data column state]
  (let [{:keys [selected edit columns]} @state
        selected? (= selected (selected-cell primary-key row-data column))]
    {:selected? selected?
     :edited? (and selected? edit)
     :width (get-in columns [(:key column) :width])}))

(defn column-width
  [columns width]
  (if width
    (str (max min-column-width width) "px")
    (str (/ 100 (count columns)) "%")))

(defn enable-edit!
  ([row-data column state]
   (enable-edit! row-data column state nil))
  ([row-data column state input]
   (when-not (:disable-edit column)
     (swap! state assoc :edit {:initial row-data
                               :updated (if input
                                          (assoc row-data (:key column) input)
                                          row-data)}))))

(defn find-index [coll v]
  (first (keep-indexed #(if (= %2 v) %1) coll)))

(defn move-candidates [columns data primary-key row-data column]
  (let [column-index (find-index (map :key columns) (:key column))
        row-index (find-index (map #(get % primary-key) data)
                              (get row-data primary-key))
        column-count (count columns)
        row-count (count data)]
    {:first-column (first columns)
     :last-column (last columns)
     :current-column column
     :previous-column (if (zero? column-index)
                        column
                        (get columns (dec column-index)))
     :next-column (if (= (dec column-count) column-index)
                    column
                    (get columns (inc column-index)))
     :first-row (first data)
     :last-row (last data)
     :current-row row-data
     :previous-row (if (zero? row-index)
                     row-data
                     (get data (dec row-index)))
     :next-row (if (= (dec row-count) row-index)
                 row-data
                 (get data (inc row-index)))}))


;; This function serves multiple purposes:
;; 1) Move selection to certain cell
;; 2) Disable editing mode, and invoke row-change-fn if necessary
;; 3) Keep focus in the correct cell
(defn move-to-cell!
  [data primary-key row-change-fn row-data column state]
  (when (:edit @state)
    (let [initial (get-in @state [:edit :initial])
          updated (get-in @state [:edit :updated])]
      (if-not (= initial updated)
        (row-change-fn (find-index (map #(get % primary-key) data)
                                   (get-in @state [:selected :row]))
                       initial
                       updated))))
  (swap! state #(assoc (dissoc % :edit)
                       :selected
                       (selected-cell primary-key row-data column)))

  ;; Dirty as fudge, but what can you do with Reagent?
  (.focus (.getElementById js/document (cell-id primary-key row-data column))))

(defn default-handle-key-down
  [e columns data primary-key row-change-fn row-data column state]
  (let [keycode (.-keyCode e)
        meta? (.-metaKey e)
        shift? (.-shiftKey e)
        selected (:selected @state)
        {:keys [first-column
                last-column
                current-column
                previous-column
                next-column
                first-row
                last-row
                current-row
                previous-row
                next-row]}
        (move-candidates columns data primary-key row-data column)
        move-fn! #(move-to-cell! data primary-key row-change-fn %1 %2 state)]
    (cond

      ;; CMD / CTRL -combinations, when edit mode is not enabled
      (and (not (:edit @state)) meta?)
      (do
        (.preventDefault e)
        (case keycode

          ;; Home selects the first cell
          36 (move-fn! first-row first-column)

          ;; Arrow keys move to the beginning of row or col
          37 (move-fn! current-row first-column)
          38 (move-fn! first-row current-column)
          39 (move-fn! current-row last-column)
          40 (move-fn! last-row current-column)
          nil))

      ;; Home moves to the beginning of row if not in edit mode
      (and (not (:edit @state)) (= keycode 36))
      (do
        (.preventDefault e)
        (move-fn! current-row first-column))

      ;; Shift + tab moves one cell backwards
      (and shift? (= keycode 9))
      (do
        (.preventDefault e)
        (move-fn! current-row previous-column))

      ;; Tab always moves one cell forward
      (= keycode 9)
      (do
        (.preventDefault e)
        (move-fn! current-row next-column))

      ;; Enter in editing mode disables it and moves selection to cell under
      (and (:edit @state) (= keycode 13))
      (do
        (.preventDefault e)
        (move-fn! next-row current-column))

      (nil? (:edit @state))
      (let [key (-> e .-key)
            from-charcode (.fromCharCode js/String keycode)]
        (.preventDefault e)
        (cond

          ;; Arrow keys in navigation mode change the selected cell
          (= keycode 37) (move-fn! current-row previous-column)
          (= keycode 38) (move-fn! previous-row current-column)
          (= keycode 39) (move-fn! current-row next-column)
          (= keycode 40) (move-fn! next-row current-column)

          ;; Enter and F2 in navigation mode enable editing mode
          (= keycode 13) (enable-edit! current-row current-column state)
          (= keycode 113) (enable-edit! current-row current-column state)

          ;; Most keycodes take the user to edit mode
          (= (count key) 1) (enable-edit! current-row current-column state key)

          ;; If key property was not available, do you best with keyCode
          (and from-charcode (re-matches #"\d|\w" from-charcode))
          (enable-edit! column
                        row-data
                        state (if shift?
                                from-charcode
                                (.toLowerCase from-charcode)))

          :else
          nil)))))

(defn move-cursor-to-end!
  [e]
  (let [el (.-target e)
        length (count (.-value el))]
    (set! (.-selectionStart el) length)
    (set! (.-selectionEnd el) length)))

(defn vertical-scrollbar-size
  [main-el]
  (let [scroll-el (aget (dom/getElementsByClass
                         "reabledit-data-rows-container"
                         main-el)
                        0)]
    (if scroll-el
      (- (.-offsetWidth scroll-el) (.-clientWidth scroll-el))
      0)))
