(ns reabledit.util)

(defn set-selected!
  [state row col]
  (swap! state assoc :selected [row col]))

(defn enable-edit!
  [data state]
  (let [row-data (nth data (first (:selected @state)))]
    (swap! state assoc :edit {:initial row-data
                              :updated row-data})))
