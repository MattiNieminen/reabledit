(ns reabledit.util)

(defn set-selected!
  [state row col]
  (swap! state dissoc :edit)
  (swap! state assoc :selected [row col]))

(defn enable-edit!
  [columns data state]
  (if-not (:disable-edit (nth columns (second (:selected @state))))
    (let [row-data (nth data (first (:selected @state)))]
      (swap! state assoc :edit {:initial row-data
                                :updated row-data}))))

(defn disable-edit!
  [state element-id]
  (swap! state dissoc :edit)

  ;; Dirty as fudge, but what can you do with Reagent?
  (.focus (.getElementById js/document element-id)))

(defn move-cursor-to-end!
  [e]
  (let [el (.-target e)
        length (count (.-value el))]
    (set! (.-selectionStart el) length)
    (set! (.-selectionEnd el) length)))
