(ns main
  (:require
    [sguis.workspaces.counter :refer [counter-ui]]
    [sguis.workspaces.temperature :refer [temperature-ui]]
    [sguis.workspaces.flight-booker :refer [booker-ui]]
    [sguis.workspaces.timer :refer [timer-ui]]
    [sguis.workspaces.crud :refer [crud-ui]]
    [sguis.workspaces.circle-drawer :refer [circles-ui]]
    [sguis.workspaces.cells :refer [cells-ui]]
    [sguis.workspaces.alternate-cells :refer [alt-cells-ui]]
    [reagent.dom :as dom]))

(defn main-component
  []
  [:div.panel
   [:div.panel-block.is-block
    [:div.container
     [counter-ui]]]
   [:div.panel-block.is-block
    [:div.container
     [temperature-ui]]]
   [:div.panel-block.is-block
    [:div.container
     [booker-ui]]]
   [:div.panel-block.is-block
    [:div.container
     [timer-ui]]]
   [:div.panel-block.is-block
    [:div.container
     [crud-ui]]]
   [:div.panel-block.is-block
    [:div.container
     [circles-ui]]]
   [:div.panel-block.is-block
    [:div.container
     [cells-ui]]]
   [:div.panel-block.is-block
    [:div.container
     [alt-cells-ui]]]])

(defn run
  []
  (dom/render
    [main-component]
    (js/document.getElementById "app")))
