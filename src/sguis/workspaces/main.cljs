(ns sguis.workspaces.main
  (:require [reagent.core :as r]
            [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [sguis.workspaces.counter :refer [counter-state
                                              counter-ui]]))

(defonce init (ws/mount))

(ws/defcard counter-card
  (ct.react/react-card
   counter-state
   (r/as-element [counter-ui counter-state])))
