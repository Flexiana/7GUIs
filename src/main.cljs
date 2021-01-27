(ns main
  (:require
   [sguis.workspaces.counter :refer [counter-ui]]
   [sguis.workspaces.temperature :refer [temperature-ui]]
   [sguis.workspaces.flight-booker :refer [booker-ui]]
   [sguis.workspaces.timer :refer [timer-ui]]
   [sguis.workspaces.crud :refer [crud-ui]]
   [sguis.workspaces.circle-drawer :refer [circles-ui]]
   [sguis.workspaces.cells :refer [cells-ui]]
   [reagent.dom :as dom]))

(defn main-component []
  [:<>
   [counter-ui]
   [temperature-ui]
   [booker-ui]
   [timer-ui]
   [crud-ui]
   [circles-ui]
   [cells-ui]])

(defn run []
  (dom/render
   [main-component]
   (js/document.getElementById "app")))
