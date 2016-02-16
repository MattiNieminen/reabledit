(ns reabledit.keyboard
  (:require [reabledit.util :as util]))

(defn handle-key-down
  [e columns data state row-change-fn element-id]
  (let [keycode (.-keyCode e)
        shift? (.-shiftKey e)
        current-row (-> @state :selected first)
        current-col (-> @state :selected second)
        rows (-> data count dec)
        cols (-> columns count dec)
        f (partial util/set-selected! state)]
    (cond

      ;; Shift + tab always moves one cell backwards (and disabled editing)
      (and shift? (= keycode 9))
      (do
        (.preventDefault e)
        (util/disable-edit! state element-id)
        (f current-row (max 0 (dec current-col))))

      ;; Tab always moves one cell forward (and disabled editing)
      (= keycode 9)
      (do
        (.preventDefault e)
        (util/disable-edit! state element-id)
        (f current-row (min cols (inc current-col))))

      ;; Enter in editing mode disables editing and invokes the callback
      (and (:edit @state) (= keycode 13))
      (do
        (.preventDefault e)
        (row-change-fn (first (:selected @state))
                       (get-in @state [:edit :initial])
                       (get-in @state [:edit :updated]))
        (util/disable-edit! state element-id))

      ;; Arrow keys in navigation mode change the selection
      ;; Enter and F2 in navigation mode enables editing mode
      (nil? (:edit @state))
      (do
        (.preventDefault e)
        (case keycode
          37 (f current-row (max 0 (dec current-col)))
          38 (f (max 0 (dec current-row)) current-col)
          39 (f current-row (min cols (inc current-col)))
          40 (f (min rows (inc current-row)) current-col)
          13 (util/enable-edit! columns data state)
          113 (util/enable-edit! columns data state)
          nil)))))
