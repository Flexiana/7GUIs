(ns sguis.workspaces.temperature
  (:require [reagent.core :as r]))

(def temperature-state
  (r/atom {}))

(defn temperature-ui [temperature-state]
  [:div "hi"])
