(ns sguis.workspaces.main
  (:require [reagent.core :as r]
            [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [sguis.workspaces.counter :refer [counter-ui
                                              *counter]]
            [sguis.workspaces.temperature :refer [temperature-ui
                                                  *temperature]]
            [sguis.workspaces.flight-booker :refer [booker-ui
                                                    *booker]]
            [sguis.workspaces.timer :refer [timer-ui
                                            *timer]]))

(defonce init (ws/mount))

(ws/defcard counter
  (ct.react/react-card
   *counter
   (r/as-element [counter-ui *counter])))

(ws/defcard temperature
  (ct.react/react-card
   *temperature
   (r/as-element [temperature-ui *temperature])))

(ws/defcard flight-booker
  (ct.react/react-card
   *booker
   (r/as-element [booker-ui *booker (js/Date.)])))

(ws/defcard timer-booker
  (ct.react/react-card
   *timer
   (r/as-element [timer-ui *timer])))
