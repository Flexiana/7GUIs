(ns sguis.workspaces.flight-booker
  (:require [reagent.core :as r]))

(def booker-state
  (r/atom {}))

(defn booker-ui [booker-state]
  [:div "hi"])
