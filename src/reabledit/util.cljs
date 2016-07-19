(ns reabledit.util
  (:require [goog.dom :as dom]))

(def min-column-width 15)

(defn header-id
  [k]
  (str "reabledit-cell--header-" (name k)))

(defn cell-state
  [primary-key state column-key row-id]
  (let [selected-row-id (get (:selected-row-data @state) primary-key)
        selected? (and (= row-id selected-row-id)
                       (= column-key (:selected-column-key @state)))]
    {:selected? selected?
     :edited? (and selected? (:edit? @state))
     :width (get (:column-widths @state) column-key)}))

(defn column-width
  [column-count width]
  (if width
    (str (max min-column-width width) "px")
    (str (/ 100 column-count) "%")))

(defn int-coercable?
  [s]
  (re-matches #"\s*((0[,.]0*)|0|([1-9][0-9]*)([.,]0*)?)\s*" s))

(defn parse-int
  [s fallback]
  (if (int-coercable? s)
    (js/parseInt s)
    fallback))

(defn find-in
  [coll match-fn v]
  (-> (filter #(match-fn % v) coll)
      first))

(defn enable-edit!
  ([state]
   (swap! state assoc :edit? true))
  ([state row-candidate]
   (swap! state assoc :edit? true :edited-row-data row-candidate)))

(defn find-index [coll v]
  (first (keep-indexed #(if (= %2 v) %1) coll)))

(defn move-to-cell!
  [primary-key row-change-fn state row-ids row-data column-key]
  (let [edit? (:edit? @state)
        selected-row-data (:selected-row-data @state)
        edited-row-data (:edited-row-data @state)]
    (when edit?
      (if-not (= selected-row-data edited-row-data)
        (row-change-fn (find-index row-ids (get selected-row-data primary-key))
                       selected-row-data
                       edited-row-data)))
    (swap! state
           assoc
           :edit?
           false
           :selected-column-key
           column-key
           :selected-row-data
           row-data
           :edited-row-data
           row-data)))

(defn handle-key-down
  [e columns data primary-key row-change-fn state]
  (let [keycode (.-keyCode e)
        meta? (.-metaKey e)
        shift? (.-shiftKey e)
        {:keys [edit? selected-column-key selected-row-data]} @state
        column-keys (map :key columns)
        column-index (find-index column-keys selected-column-key)
        column-count (count columns)
        row-ids (map #(get % primary-key) data)
        row-index (find-index row-ids (get selected-row-data primary-key))
        row-count (count data)
        first-column-key (-> columns first :key)
        last-column-key (-> columns last :key)
        column-key-on-left (if (zero? column-index)
                             selected-column-key
                             (:key (get columns (dec column-index))))
        column-key-on-right (if (= (dec column-count) column-index)
                              selected-column-key
                              (:key (get columns (inc column-index))))
        first-row (first data)
        last-row (last data)
        row-above (if (zero? row-index)
                    selected-row-data
                    (get data (dec row-index)))
        row-below (if (= (dec row-count) row-index)
                    selected-row-data
                    (get data (inc row-index)))
        move-fn! (fn [column-key row-data]
                   (.preventDefault e)
                   (move-to-cell! primary-key
                                  row-change-fn
                                  state
                                  row-ids
                                  row-data
                                  column-key))
        enable-edit! (fn []
                       (.preventDefault e)
                       (enable-edit! state))]
    (cond

      ;; Shift + tab moves one cell backwards
      (and shift? (= keycode 9))
      (move-fn! column-key-on-left selected-row-data)

      ;; Tab always moves one cell forward
      (= keycode 9)
      (move-fn! column-key-on-right selected-row-data)

      ;; Enter in editing mode disables it and moves selection to cell under
      (and edit? (= keycode 13))
      (move-fn! selected-column-key row-below)

      (not edit?)
      (do
        (cond

          ;; CMD / CTRL -combinations when in navigation mode
          meta?
          (case keycode

            ;; Home selects the first cell
            36 (move-fn! first-column-key first-row)

            ;; Arrow keys move to the beginning of row or col
            37 (move-fn! first-column-key selected-row-data)
            38 (move-fn! selected-column-key first-row)
            39 (move-fn! last-column-key selected-row-data)
            40 (move-fn! selected-column-key last-row)
            nil)

          ;; Arrow keys in navigation mode change the selected cell
          (= keycode 37) (move-fn! column-key-on-left selected-row-data)
          (= keycode 38) (move-fn! selected-column-key row-above)
          (= keycode 39) (move-fn! column-key-on-right selected-row-data)
          (= keycode 40) (move-fn! selected-column-key row-below)

          ;; Home in navigation mode moves to the beginning of row
          (= keycode 36) (move-fn! first-column-key selected-row-data)

          ;; Enter and F2 in navigation mode enable editing mode
          (= keycode 13) (enable-edit!)
          (= keycode 113) (enable-edit!)

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
                         "reabledit-data-rows"
                         main-el)
                        0)]
    (if scroll-el
      (- (.-offsetWidth scroll-el) (.-clientWidth scroll-el))
      0)))

(defn start-resize!
  [e k state]
  (swap! state assoc :resize k)
  (.setData (.-dataTransfer e) "Text" (name k))
  (set! (-> e .-dataTransfer .-effectAllowed) "move"))

(defn stop-resize!
  [state]
  (swap! state dissoc :resize))

(defn resize!
  [e state]
  (let [k (:resize @state)
        element (.getElementById js/document (header-id k))
        width (- (.-pageX e) (.-left (.getBoundingClientRect element)))]
    (swap! state assoc-in [:column-widths k] width)))

(defn get-clipboard-data
  [e]
  (.preventDefault e)
  (.getData (.-clipboardData e) "text/plain"))

(defn set-clipboard-data
  [e v]
  (.preventDefault e)
  (.setData (.-clipboardData e) "text/plain" v))
