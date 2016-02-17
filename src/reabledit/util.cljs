(ns reabledit.util)

(defn set-selected-fn
  [state disable-edit!]
  (fn [row col]
    (disable-edit!)
    (swap! state assoc :selected [row col])))

(defn enable-edit-fn
  [columns data state]
  (fn []
    (if-not (:disable-edit (nth columns (second (:selected @state))))
      (let [row-data (nth data (first (:selected @state)))]
        (swap! state assoc :edit {:initial row-data
                                  :updated row-data})))))

(defn disable-edit-fn
  [state row-change-fn element-id]
  (fn []
    (let [initial (get-in @state [:edit :initial])
          updated (get-in @state [:edit :updated])]
      (when (:edit @state)
        (if-not (= initial updated)
          (row-change-fn (first (:selected @state)) initial updated))
        (swap! state dissoc :edit)

        ;; Dirty as fudge, but what can you do with Reagent?
        (.focus (.getElementById js/document element-id))))))

(defn move-cursor-to-end!
  [e]
  (let [el (.-target e)
        length (count (.-value el))]
    (set! (.-selectionStart el) length)
    (set! (.-selectionEnd el) length)))
