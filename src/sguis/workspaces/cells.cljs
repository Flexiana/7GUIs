(ns sguis.workspaces.cells
  (:require [reagent.core :as r]))

(def *cells
  (r/atom {}))

(def a->z
  (map char (range 97 123)))

(defn cells-ui [cells-state]
  [:table])
