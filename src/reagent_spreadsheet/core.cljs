(ns reagent-spreadsheet.core
  (:require
   [reagent.core :as reagent])
  (:require-macros
   [devcards.core :as dc :refer [defcard]]))

(defcard my-first-card
  (dc/reagent
   [:h1 "Devcards is freaking awesome!"]))
