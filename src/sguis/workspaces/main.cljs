(ns sguis.workspaces.main
  (:require
    [nubank.workspaces.card-types.react :refer [react-card]]
    [nubank.workspaces.core :refer [mount defcard]]
    [nubank.workspaces.model :as wsm]
    [reagent.core :as r]
    [sguis.workspaces.cells :refer [cells-ui]]
    [sguis.workspaces.cells-test]
    [sguis.workspaces.circle-drawer :refer [circles-ui]]
    [sguis.workspaces.circle-drawer-test]
    [sguis.workspaces.counter :refer [counter-ui]]
    [sguis.workspaces.counter-test]
    [sguis.workspaces.crud :refer [crud-ui]]
    [sguis.workspaces.crud-test]
    [sguis.workspaces.flight-booker :refer [booker-ui]]
    [sguis.workspaces.flight-booker-test]
    [sguis.workspaces.temperature :refer [temperature-ui]]
    [sguis.workspaces.temperature-test]
    [sguis.workspaces.timer :refer [timer-ui]]
    [sguis.workspaces.timer-test]
    [sguis.workspaces.eval-cell-test]))

(defonce init (mount))

(defcard counter
  {::wsm/align       {:justify-content "left"}}
  (react-card
    (r/as-element [counter-ui])))

(defcard temperature
  {::wsm/align       {:justify-content "left"}}
  (react-card
    (r/as-element [temperature-ui])))

(defcard flight-booker
  {::wsm/align       {:justify-content "left"}}
  (react-card
    (r/as-element [booker-ui])))

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
