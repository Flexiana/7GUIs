(ns sguis.main
  (:require [reagent.core :as r]
            [nubank.workspaces.card-types.react :refer [react-card]]
            [nubank.workspaces.core :refer [mount defcard]]
            [nubank.workspaces.model :as wsm]
            [sguis.workspaces.counter :refer [counter-ui]]
            [sguis.workspaces.counter-test]
            [sguis.workspaces.temperature :refer [temperature-ui]]
            [sguis.workspaces.temperature-test]
            [sguis.workspaces.flight-booker :refer [booker-ui]]
            [sguis.workspaces.flight-booker-test]
            [sguis.workspaces.timer :refer [timer-ui]]
            [sguis.workspaces.timer-test]
            [sguis.workspaces.crud :refer [crud-ui]]
            [sguis.workspaces.crud-test]
            [sguis.workspaces.circle-drawer :refer [circles-ui]]
            [sguis.workspaces.circle-drawer-test]
            [sguis.workspaces.cells :refer [cells-ui]]
            [sguis.workspaces.cells-test]))

(defonce init (mount))

(defcard counter
  (react-card
   (r/as-element [counter-ui])))

(defcard temperature
  (react-card
    (r/as-element [temperature-ui])))

(defcard flight-booker
  (react-card
   (r/as-element [booker-ui (js/Date.)])))

(defcard crud
  (react-card
   (r/as-element [crud-ui])))

(defcard timer
  (react-card
   (r/as-element [timer-ui])))

(defcard circle-drawer
  {::wsm/align       {:justify-content "left"}}
  (react-card
   (r/as-element [circles-ui])))

(defcard cells
  {::wsm/align       {:justify-content "left"}}
  (react-card
   (r/as-element [cells-ui])))
