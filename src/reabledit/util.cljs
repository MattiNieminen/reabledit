(ns reabledit.util)

(defn update-row
  [data nth-row new-row]
  (let [[init tail] (split-at nth-row data)]
    (into [] (concat init [new-row] (rest tail)))))
