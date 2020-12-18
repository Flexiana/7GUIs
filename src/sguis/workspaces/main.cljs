(ns sguis.workspaces.main
  (:require [reagent.core :as r]
            [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [sguis.workspaces.counter :as counter]))

(defonce init (ws/mount))

(ws/defcard counter-example-card
  (ct.react/react-card
   counter/counter-state
   (r/as-element [counter/counter-ui counter/counter-state])))
