(ns reabledit.util)

(defn cell-id
  [nth-row nth-col]
  (str "reabledit-cell-" nth-row "-" nth-col))

(defn cell-info
  [nth-row nth-col state]
  (let [{:keys [selected edit]} @state]
    (if (= selected [nth-row nth-col])
      {:selected? true
       :edited? (boolean edit)})))

(defn enable-edit!
  [column row-data state]
  (if-not (:disable-edit column)
    (swap! state assoc :edit {:initial row-data
                              :updated row-data})))

;; This function serves multiple purposes:
;; 1) Move selection to certain cell
;; 2) Disable editing mode, and invoke row-change-fn if necessary
;; 3) Keep focus in the correct cell
(defn move-to-cell!
  [row-change-fn nth-row nth-col state]
  (when (:edit @state)
    (let [initial (get-in @state [:edit :initial])
          updated (get-in @state [:edit :updated])]
      (if-not (= initial updated)
        (row-change-fn (first (:selected @state)) initial updated))))
  (reset! state {:selected [nth-row nth-col]})

  ;; Dirty as fudge, but what can you do with Reagent?
  (.focus (.getElementById js/document (cell-id nth-row nth-col))))

(defn default-handle-key-down
  [e row-change-fn column row-data rows cols state]
  (let [keycode (.-keyCode e)
        meta? (.-metaKey e)
        shift? (.-shiftKey e)
        current-row (-> @state :selected first)
        current-col (-> @state :selected second)]
    (cond

      ;; CMD / CTRL -combinations, when edit mode is not enabled
      (and (not (:edit @state)) meta?)
      (do
        (.preventDefault e)
        (case keycode

          ;; Home selects the first cell
          36 (move-to-cell! row-change-fn 0 0 state)

          ;; Arrow keys move to the beginning of row or col
          37 (move-to-cell! row-change-fn current-row 0 state)
          38 (move-to-cell! row-change-fn 0 current-col state)
          39 (move-to-cell! row-change-fn current-row cols state)
          40 (move-to-cell! row-change-fn rows current-col state)
          nil
          ))

      ;; Home moves to the beginning of row if not in edit mode
      (and (not (:edit @state)) (= keycode 36))
      (do
        (.preventDefault e)
        (move-to-cell! row-change-fn
                       current-row
                       0
                       state))

      ;; Shift + tab moves one cell backwards
      (and shift? (= keycode 9))
      (do
        (.preventDefault e)
        (move-to-cell! row-change-fn
                       current-row
                       (max 0 (dec current-col))
                       state))

      ;; Tab always moves one cell forward
      (= keycode 9)
      (do
        (.preventDefault e)
        (move-to-cell! row-change-fn
                       current-row
                       (min cols (inc current-col))
                       state))

      ;; Enter in editing mode disables it and moves selection to cell under
      (and (:edit @state) (= keycode 13))
      (do
        (.preventDefault e)
        (move-to-cell! row-change-fn
                       (min rows (inc current-row))
                       current-col
                       state))

      ;; Arrow keys in navigation mode change the selected cell
      ;; Enter and F2 in navigation mode enable editing mode
      (nil? (:edit @state))
      (do
        (.preventDefault e)
        (case keycode
          37 (move-to-cell! row-change-fn
                            current-row
                            (max 0 (dec current-col))
                            state)
          38 (move-to-cell! row-change-fn
                            (max 0 (dec current-row))
                            current-col
                            state)
          39 (move-to-cell! row-change-fn
                            current-row
                            (min cols (inc current-col))
                            state)
          40 (move-to-cell! row-change-fn
                            (min rows (inc current-row))
                            current-col
                            state)
          13 (enable-edit! column row-data state)
          113 (enable-edit! column row-data state)
          nil)))))

(defn move-cursor-to-end!
  [e]
  (let [el (.-target e)
        length (count (.-value el))]
    (set! (.-selectionStart el) length)
    (set! (.-selectionEnd el) length)))
