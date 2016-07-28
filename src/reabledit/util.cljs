(ns reabledit.util
  (:require [goog.dom :as dom]))

(def min-column-width 15)

(defn header-id
  [k]
  (str "reabledit-cell--header-" (name k)))

(defn cell-state
  [state column-key row-id]
  {:selected? (and (= row-id (:selected-row-id @state))
                   (= column-key (:selected-column-key @state)))
   :width (get (:column-widths @state) column-key)})

(defn column-width
  [column-count width]
  (if width
    (str (max min-column-width width) "px")
    (str (/ 100 column-count) "%")))

(defn find-index [coll v]
  (first (keep-indexed #(if (= %2 v) %1) coll)))

(defn move-to-cell!
  [state row-id column-key]
  (swap! state assoc :selected-row-id row-id :selected-column-key column-key))

(defn fix-focus!
  [cell-el]
  (.focus (dom/getElementByClass "reabledit-focused" cell-el)))

(defn handle-key-down
  [e state column-keys row-ids]
  (let [keycode (.-keyCode e)
        meta? (.-metaKey e)
        shift? (.-shiftKey e)
        {:keys [selected-column-key selected-row-id]} @state
        column-index (find-index column-keys selected-column-key)
        column-count (count column-keys)
        row-index (find-index row-ids selected-row-id)
        row-count (count row-ids)
        first-column-key (first column-keys)
        last-column-key (last column-keys)
        column-key-on-left (if (zero? column-index)
                             selected-column-key
                             (get column-keys (dec column-index)))
        column-key-on-right (if (= (dec column-count) column-index)
                              selected-column-key
                              (get column-keys (inc column-index)))
        first-row-id (first row-ids)
        last-row-id (last row-ids)
        row-id-above (if (zero? row-index)
                       selected-row-id
                       (get row-ids (dec row-index)))
        row-id-below (if (= (dec row-count) row-index)
                       selected-row-id
                       (get row-ids (inc row-index)))
        move-fn! (fn [row-id column-key]
                   (.preventDefault e)
                   (move-to-cell! state row-id column-key))]
    (cond

      ;; Shift + tab moves one cell backwards
      (and shift? (= keycode 9))
      (move-fn! selected-row-id column-key-on-left)

      ;; Tab moves one cell forward
      (= keycode 9)
      (move-fn! selected-row-id column-key-on-right)

      ;; Enter moves one cell below
      (= keycode 13)
      (move-fn! row-id-below selected-column-key)

      ;; CMD / CTRL -combinations
      meta?
      (case keycode

        ;; Home selects the first cell
        36 (move-fn! first-row-id first-column-key)

        ;; Arrow keys move to the beginning of row or col
        37 (move-fn! selected-row-id first-column-key)
        38 (move-fn! first-row-id selected-column-key)
        39 (move-fn! selected-row-id last-column-key)
        40 (move-fn! last-row-id selected-column-key)
        nil)

      ;; Arrow keys move one cell
      (= keycode 37) (move-fn! selected-row-id column-key-on-left)
      (= keycode 38) (move-fn! row-id-above selected-column-key)
      (= keycode 39) (move-fn! selected-row-id column-key-on-right)
      (= keycode 40) (move-fn! row-id-below selected-column-key)

      ;; Home moves to the beginning of row
      (= keycode 36) (move-fn! selected-row-id first-column-key)

      :else
      nil)))

(defn move-cursor-to-end!
  [e]
  (let [el (.-target e)
        length (count (.-value el))]
    (set! (.-selectionStart el) length)
    (set! (.-selectionEnd el) length)))

(defn post-render
  [main-el]
  (let [data-rows-el (dom/getElementByClass "reabledit-data-rows" main-el)
        header-row-el (dom/getElementByClass "reabledit-row--header" main-el)
        header-scroll-el (dom/getElementByClass "reabledit-cell__header-scroll"
                                                main-el)
        _ (aset (.-style data-rows-el)
                "height"
                (str "calc(100% - " (.-clientHeight header-row-el) "px)"))
        scrollbar-width (- (.-offsetWidth data-rows-el) (.-clientWidth data-rows-el))]
    (aset data-rows-el "onscroll" (fn [e]
                                    (when (= (.-currentTarget e) (.-target e))
                                      (set! (.-scrollLeft header-row-el)
                                            (-> e .-target .-scrollLeft)))))
    (aset (.-style header-row-el) "padding-right" (str scrollbar-width "px"))
    (aset (.-style header-scroll-el) "min-width" (str scrollbar-width "px"))))

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
