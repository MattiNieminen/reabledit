(ns devdemo.core
  (:require [reagent.core :as reagent]
            [reagent-spreadsheet.core :as spreadsheet])
  (:require-macros [devcards.core :as dc :refer [defcard]]))

(defcard reagent-spreadsheet
  (dc/reagent
   [spreadsheet/spreadsheet]))
