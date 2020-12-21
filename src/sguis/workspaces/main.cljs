(ns sguis.workspaces.main
  (:require [reagent.core :as r]
            [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [sguis.workspaces.counter :refer [counter-state
                                              counter-ui]]
            [sguis.workspaces.temperature :refer [temperature-state
                                                  temperature-ui]]
            [sguis.workspaces.flight-booker :refer [booker-ui
                                                    *booker]]))
(defonce init (ws/mount))

(ws/defcard counter-card
  (ct.react/react-card
   counter-state
   (r/as-element [counter-ui counter-state])))


(ws/defcard temperature-card
  (ct.react/react-card
   temperature-state
   (r/as-element [temperature-ui temperature-state])))

(ws/defcard flight-booker
  (ct.react/react-card
   *booker
   (r/as-element [booker-ui *booker (js/Date.)])))
