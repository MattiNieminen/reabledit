(ns reabledit.keyboard
  (:require [reabledit.util :as util]))

(defn handle-key-down
  [e columns data state enable-edit! disable-edit! set-selected!]
  (let [keycode (.-keyCode e)
        shift? (.-shiftKey e)
        current-row (-> @state :selected first)
        current-col (-> @state :selected second)
        rows (-> data count dec)
        cols (-> columns count dec)]
    (cond

      ;; Shift + tab always moves one cell backwards (and disabled editing)
      (and shift? (= keycode 9))
      (do
        (.preventDefault e)
        (disable-edit!)
        (set-selected! current-row (max 0 (dec current-col))))

      ;; Tab always moves one cell forward (and disabled editing)
      (= keycode 9)
      (do
        (.preventDefault e)
        (disable-edit!)
        (set-selected! current-row (min cols (inc current-col))))

      ;; Enter in editing mode disables editing and invokes the callback
      (and (:edit @state) (= keycode 13))
      (do
        (.preventDefault e)
        (disable-edit!))

      ;; Arrow keys in navigation mode change the selection
      ;; Enter and F2 in navigation mode enables editing mode
      (nil? (:edit @state))
      (do
        (.preventDefault e)
        (case keycode
          37 (set-selected! current-row (max 0 (dec current-col)))
          38 (set-selected! (max 0 (dec current-row)) current-col)
          39 (set-selected! current-row (min cols (inc current-col)))
          40 (set-selected! (min rows (inc current-row)) current-col)
          13 (enable-edit!)
          113 (enable-edit!)
          nil)))))
