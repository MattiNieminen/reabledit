(ns reabledit.components
  (:require [reabledit.util :as util]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [goog.dom.classlist :as classlist]
            [goog.dom :as dom]))

;;
;; Cell views
;;

(defn default-view
  [row-data k change-directly! enable-edit! _]
  (let [v (get row-data k)]
    [:div.reabledit-cell-view
     [:span.reabledit-cell__content
      v]
     [:input.reabledit-cell-view__hidden-input-handler.reabledit-focused
      {:value nil
       :on-change #(enable-edit! (assoc row-data k (-> % .-target .-value)))
       :on-copy #(util/set-clipboard-data % v)
       :on-paste #(change-directly! (assoc row-data
                                           k
                                           (util/get-clipboard-data %)))
       :on-cut (fn [e]
                 (util/set-clipboard-data e v)
                 (change-directly! (assoc row-data k nil)))}]]))

(defn dropdown-view
  [row-data k change-directly! enable-edit! {:keys [options]}]
  (let [v (-> (filter #(= (:key %) (get row-data k)) options)
              first
              :value)]
    [:div.reabledit-cell-view
     [:span.reabledit-cell__content
      v]
     [:input.reabledit-cell-view__hidden-input-handler.reabledit-focused
      {:value nil
       :on-change (fn [e]
                    (let [input (str/lower-case (-> e .-target .-value))
                          match-fn #(str/starts-with? (-> %1
                                                          :value
                                                          str/lower-case)
                                                      %2)
                          o (util/find-in options match-fn input)]
                      (enable-edit! (assoc row-data k (:key o)))))
       :on-copy #(util/set-clipboard-data % v)
       :on-paste (fn [e]
                   (let [input (str/lower-case (util/get-clipboard-data e))
                         match-fn #(= (-> %1 :value str/lower-case) %2)
                         o (util/find-in options match-fn input)]
                     (change-directly! (assoc row-data k (:key o)))))
       :on-cut #(util/set-clipboard-data % v)}]]))

;;
;; Cell editors
;;

(defn default-editor
  [_ edited-row-data k change-edited! _ _]
  [:input.reabledit-cell-editor-input
   {:type "text"
    :auto-focus true
    :on-focus util/move-cursor-to-end!
    :value (get edited-row-data k)
    :on-change #(change-edited! (assoc edited-row-data
                                  k
                                  (-> % .-target .-value)))}])

(defn int-editor
  [initial-row-data edited-row-data k change-edited! _ _]
  (let [initial-value (get initial-row-data k)
        input-candidate (str (get edited-row-data k))
        parsed (util/parse-int input-candidate initial-value)
        input (reagent/atom input-candidate)]
    (change-edited! (assoc edited-row-data
                           k
                           parsed))
    (fn [initial-row-data edited-row-data k change-edited! _ _]
      [:input.reabledit-cell-editor-input
       {:type "text"
        :class (if-not (util/int-coercable? @input)
                 "reabledit-cell-editor-input--error")
        :auto-focus true
        :on-focus util/move-cursor-to-end!
        :value @input
        :on-change (fn [e]
                     (let [new-input (-> e .-target .-value)
                           parsed (util/parse-int new-input initial-value)]
                       (reset! input new-input)
                       (change-edited! (assoc edited-row-data
                                              k
                                              parsed))))}])))

(defn- dropdown-editor-key-down
  [e v change-edited! options]
  (let [keycode (.-keyCode e)
        position (first (keep-indexed #(if (= %2 v) %1)
                                      (map :key options)))]
    (case keycode
      38 (do
           (.preventDefault e)
           (if (zero? position)
             (change-edited! (-> options last :key))
             (change-edited! (:key (nth options (dec position))))))
      40 (do
           (.preventDefault e)
           (if (= position (-> options count dec))
             (change-edited! (-> options first :key))
             (change-edited! (:key (nth options (inc position))))))
      nil)))

(defn dropdown-editor
  [_ edited-row-data k change-edited! _ disable-edit! {:keys [options]}]
  (reagent/create-class
   {:component-did-mount #(.focus (reagent/dom-node %))
    :reagent-render
    (fn [_ edited-row-data k change-edited! _ disable-edit! {:keys [options]}]
      (let [v (get edited-row-data k)
            change-edited! #(change-edited! (assoc edited-row-data k %))]
        [:div.reabledit-cell-editor-dropdown
         {:tabIndex 0
          :on-key-down #(dropdown-editor-key-down % v change-edited! options)}
         [:span.reabledit-cell__content
          (-> (filter #(= (:key %) v) options)
              first
              :value)]
         [:div.reabledit-cell-editor-dropdown-list
          (for [{:keys [key value]} options]
            ^{:key key}
            [:div.reabledit-cell-editor-dropdown-list__item
             {:class (if (= key v)
                       "reabledit-cell-editor-dropdown-list__item--selected")
              :on-click (fn [e]
                          (.stopPropagation e)
                          (change-edited! key)
                          (disable-edit!))}
             [:span.reabledit-cell__content value]])]]))}))

;;
;; Dependencies for the main component
;;

(defn data-table-cell
  [{:keys [primary-key row-change-fn state
           column-keys row-ids row-data column]}]
  (reagent/create-class
   {:component-did-update
    (fn [this _]
      (if (classlist/contains (reagent/dom-node this)
                              "reabledit-cell--selected")
        (.focus (aget (dom/getElementsByClass "reabledit-focused"
                                              (reagent/dom-node this))
                      0))))
    :reagent-render
    (fn [{:keys [primary-key row-change-fn state
                 column-keys row-ids row-data column]}]
      (let [column-key (:key column)
            row-id (get row-data primary-key)
            {:keys [selected? edited? width]} @(reagent/track util/cell-state
                                                              primary-key
                                                              state
                                                              column-key
                                                              row-id)
            change-this-row! (partial row-change-fn
                                      (util/find-index row-ids row-id)
                                      row-data)
            enable-edit! (partial util/enable-edit! state)
            disable-edit-and-select-this-cell! #(util/move-to-cell! primary-key
                                                                    row-change-fn
                                                                    state
                                                                    row-ids
                                                                    row-data
                                                                    column-key)]
        [:div.reabledit-cell
         {:id (str "reabledit-cell-" row-id "-" column-key)
          :class (if selected? "reabledit-cell--selected")
          :style {:width (util/column-width (count column-keys) width)}
          :on-click #(if-not edited?
                       (disable-edit-and-select-this-cell!))
          :on-double-click enable-edit!}
         (if edited?
           [(or (:editor column) default-editor)
            row-data
            (get @state :edited-row-data)
            column-key
            #(swap! state assoc :edited-row-data %)
            change-this-row!
            disable-edit-and-select-this-cell!
            (:opts column)]
           [(or (:view column) default-view)
            row-data
            column-key
            change-this-row!
            enable-edit!
            (:opts column)])]))}))

(defn data-table-row
  [{:keys [columns primary-key row-change-fn state
           column-keys row-ids row-data]}]
  [:div.reabledit-row
   (for [column columns]
     ^{:key (:key column)}
     [data-table-cell {:primary-key primary-key
                       :row-change-fn row-change-fn
                       :state state
                       :column-keys column-keys
                       :row-ids row-ids
                       :row-data row-data
                       :column column}])])

(defn data-table-headers
  [{:keys [columns state]}]
  (let [column-widths (:column-widths @state)
        scrollbar-size (util/vertical-scrollbar-size (:main-el @state))]
    [:div.reabledit-row.reabledit-row--header
     (if (:resize @state)
       [:div.reabledit-resize-area
        {:on-drag-over (fn [e]
                         (.preventDefault e)
                         (util/resize! e state))
         :on-drop (fn [e]
                    (.preventDefault e)
                    (util/stop-resize! state))}])
     (for [{:keys [key value]} columns]
       ^{:key key}
       [:div.reabledit-cell.reabledit-cell--header
        {:id (util/header-id key)
         :style {:width (util/column-width (count columns)
                                           (get column-widths key))}}
        [:span.reabledit-cell__content.reabledit-cell__content--header value]
        [:div.reabledit-cell__header-handle
         {:draggable true
          :on-drag-start #(util/start-resize! % key state)
          :on-drag-end #(util/stop-resize! state)}]])
     (if (> scrollbar-size 0)
       [:div.reabledit-cell__header-scroll
        {:style {:min-width (str scrollbar-size "px")}}])]))
