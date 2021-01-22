(ns sguis.main
  (:require [reagent.core :as r]
            [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [sguis.workspaces.counter :refer [counter-ui]]
            [sguis.workspaces.counter-test]
            [sguis.workspaces.temperature :refer [temperature-ui]]
            [sguis.workspaces.temperature-test]
            [sguis.workspaces.flight-booker :refer [booker-ui
                                                    *booker]]
            [sguis.workspaces.flight-booker-test]
            [sguis.workspaces.timer :refer [timer-ui]]
            [sguis.workspaces.timer-test]
            [sguis.workspaces.crud :refer [crud-ui]]
            [sguis.workspaces.crud-test]
            [sguis.workspaces.circle-drawer :refer [circles-ui]]
            [sguis.workspaces.circle-drawer-test]
            [sguis.workspaces.cells :refer [cells-ui
                                            *cells]]
            [sguis.workspaces.cells-test]))

(defonce init (ws/mount))

(ws/defcard counter
  (ct.react/react-card
   (r/as-element [counter-ui])))

(ws/defcard temperature
  (ct.react/react-card
    (r/as-element [temperature-ui])))

(ws/defcard flight-booker
  (ct.react/react-card
   *booker
   (r/as-element [booker-ui *booker (js/Date.)])))

(ws/defcard crud
  (ct.react/react-card
   (r/as-element [crud-ui])))

(ws/defcard timer
  (ct.react/react-card
   (r/as-element [timer-ui])))

(ws/defcard circle-drawer
  {::wsm/align       {:justify-content "left"}}
  (ct.react/react-card
   (r/as-element [circles-ui])))

(ws/defcard cells
  {::wsm/align       {:justify-content "left"}}
  (ct.react/react-card
   *cells
   (r/as-element [cells-ui *cells])))
