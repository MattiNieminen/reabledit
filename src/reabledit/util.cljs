(ns reabledit.util
  (:require [goog.dom :as dom]))

(def min-column-width 15)

(defn header-id
  [k]
  (str "reabledit-cell--header-" (name k)))

(defn cell-id
  [column-key row-id]
  (str "reabledit-cell-" row-id "-" column-key))

(defn selected-cell [column-key row-id]
  {:column column-key
   :row row-id})

(defn cell-info
  [state column-key row-id]
  (let [{:keys [selected edit columns]} @state
        selected? (= selected (selected-cell column-key row-id))]
    {:selected? selected?
     :edited? (and selected? edit)
     :width (get-in columns [column-key :width])}))

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

(defn enable-edit!
  ([state row-data column]
   (enable-edit! state row-data column nil))
  ([state row-data column input-candidate]
   (when-not (:disable-edit column)
     (swap! state
            assoc
            :edit
            {:initial row-data
             :updated (if input-candidate
                        (assoc row-data (:key column) input-candidate)
                        row-data)}))))

(defn find-index [coll v]
  (first (keep-indexed #(if (= %2 v) %1) coll)))

(defn move-candidates [column-keys row-ids column-key row-id]
  (let [column-index (find-index column-keys column-key)
        row-index (find-index row-ids row-id)
        column-count (count column-keys)
        row-count (count row-ids)]
    {:first-column (first column-keys)
     :last-column (last column-keys)
     :current-column column-key
     :previous-column (if (zero? column-index)
                        column-key
                        (get column-keys (dec column-index)))
     :next-column (if (= (dec column-count) column-index)
                    column-key
                    (get column-keys (inc column-index)))
     :first-row (first row-ids)
     :last-row (last row-ids)
     :current-row row-id
     :previous-row (if (zero? row-index)
                     row-id
                     (get row-ids (dec row-index)))
     :next-row (if (= (dec row-count) row-index)
                 row-id
                 (get row-ids (inc row-index)))}))


;; This function serves multiple purposes:
;; 1) Move selection to certain cell
;; 2) Disable editing mode, and invoke row-change-fn if necessary
;; 3) Keep focus in the correct cell
(defn move-to-cell!
  [row-change-fn state row-ids column-key row-id]
  (when (:edit @state)
    (let [initial (get-in @state [:edit :initial])
          updated (get-in @state [:edit :updated])]
      (if-not (= initial updated)
        (row-change-fn (find-index row-ids (get-in @state [:selected :row]))
                       initial
                       updated))))
  (swap! state #(assoc (dissoc % :edit)
                       :selected
                       (selected-cell column-key row-id)))

  ;; Dirty as fudge, but what can you do with Reagent?
  ;; Need to wait that cell is certainly rendered
  (js/setTimeout
   #(.focus (aget (.getElementsByClassName
                   (.getElementById js/document (cell-id column-key row-id))
                   "reabledit-focused")
                  0))
   50))

(defn default-handle-key-down
  [e row-change-fn state column-keys row-ids row-data column column-key row-id]
  (let [keycode (.-keyCode e)
        meta? (.-metaKey e)
        shift? (.-shiftKey e)
        selected (:selected @state)
        {:keys [first-column
                last-column
                current-column
                previous-column
                next-column
                first-row
                last-row
                current-row
                previous-row
                next-row]}
        (move-candidates column-keys row-ids column-key row-id)
        move-fn! (fn [column row]
                   (.preventDefault e)
                   (move-to-cell! row-change-fn state row-ids column row))
        enable-edit! (fn []
                       (.preventDefault e)
                       (enable-edit! state row-data column))]
    (cond

      ;; CMD / CTRL -combinations, when edit mode is not enabled
      (and (not (:edit @state)) meta?)
      (case keycode

        ;; Home selects the first cell
        36 (move-fn! first-column first-row)

        ;; Arrow keys move to the beginning of row or col
        37 (move-fn! first-column current-row)
        38 (move-fn! current-column first-row)
        39 (move-fn! last-column current-row)
        40 (move-fn! current-column last-row)
        nil)

      ;; Home moves to the beginning of row if not in edit mode
      (and (not (:edit @state)) (= keycode 36))
      (move-fn! first-column current-row)

      ;; Shift + tab moves one cell backwards
      (and shift? (= keycode 9))
      (move-fn! previous-column current-row)

      ;; Tab always moves one cell forward
      (= keycode 9)
      (move-fn! next-column current-row)

      ;; Enter in editing mode disables it and moves selection to cell under
      (and (:edit @state) (= keycode 13))
      (move-fn! current-column next-row)

      (nil? (:edit @state))
      (let [key (-> e .-key)
            from-charcode (.fromCharCode js/String keycode)]
        (cond

          ;; Arrow keys in navigation mode change the selected cell
          (= keycode 37) (move-fn! previous-column current-row)
          (= keycode 38) (move-fn! current-column previous-row)
          (= keycode 39) (move-fn! next-column current-row)
          (= keycode 40) (move-fn! current-column next-row)

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
    (swap! state assoc-in [:columns k :width] width)))


(defn default-copy
  [e]
  (js/console.log "Not yet implemented: copy"))

(defn default-paste
  [e]
  (js/console.log "Not yet implemented: paste"))

(defn default-cut
  [e]
  (js/console.log "Not yet implemented: cut"))
