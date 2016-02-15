(ns reabledit.keyboard)

(defn handle-editing-mode-key-down
  [e state row-change-fn id]
  (if (= 13 (.-keyCode e))
    (do
      (.preventDefault e)
      (row-change-fn (first (:selected @state))
                     (get-in @state [:edit :initial])
                     (get-in @state [:edit :updated]))
      (swap! state dissoc :edit)
      ;; Dirty as fudge, but what can you do with Reagent?
      (.focus (.getElementById js/document id)))
    nil))

(defn handle-selection-mode-key-down
  [e columns data state]
  (.preventDefault e)
  (let [keycode (.-keyCode e)
        current-row (-> @state :selected first)
        current-col (-> @state :selected second)
        rows (-> data count dec)
        cols (-> columns count dec)
        f (partial set-selected! state)]
    (case keycode
      37 (f current-row (max 0 (dec current-col)))
      38 (f (max 0 (dec current-row)) current-col)
      39 (f current-row (min cols (inc current-col)))
      40 (f (min rows (inc current-row)) current-col)
      9 (f current-row (min cols (inc current-col)))
      13 (enable-edit! data state)
      nil)))

(defn handle-key-down
  [e columns data state row-change-fn id]
  (if (:edit @state)
    (handle-editing-mode-key-down e state row-change-fn id)
    (handle-selection-mode-key-down e columns data state)))
