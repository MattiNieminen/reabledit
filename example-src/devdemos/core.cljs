(ns devdemos.core
  (:require [reagent.core :as reagent]
            [reabledit.core :as reabledit])
  (:require-macros [devcards.core :as dc :refer [defcard]]))

(defcard reagent-spreadsheet
  (dc/reagent
   [reabledit/table-edit]))
