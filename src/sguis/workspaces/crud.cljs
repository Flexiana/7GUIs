(ns sguis.workspaces.crud
  (:require [reagent.core :as r]))

(def *crud
  (r/atom {}))

(def button-style
  {:margin-right  "20px"
   :cursor        "pointer"
   :border-radius "50px"})

(defn filter-prefix [crud-state]
  [:div {:padding "1em"}
   [:label "Filter prefix: "]
   [:input {:type "text"}]])

(defn insert-ui [crud-state]
  [:div {:padding "1em"}
   [:label "Name: "
    [:input {:type "text"}]]
   [:label "Surname: "
    [:input {:type "text"}]]])

(defn read-ui [crud-state]
  [:div
   [:span "Surname"]
   [:span ", "]
   [:span "Name"]])

(defn people-ui [crud-state]
  [:div {:style {:display         "flex"
                 :justify-content "space-between"}}
   [:div {:style {:width  "100%"
                  :height "100%"
                  :border "1px solid gray"}}
    [read-ui crud-state]]
   [:div {:style {:display        "flex"
                  :padding        "1em"
                  :flex-direction "column"}}
    [insert-ui crud-state]]])

(defn create-person [crud-state]
  [:button {:style button-style}
   "create"])
(defn update-person [crud-state]
  [:button {:style button-style}
   "update"])

(defn delete-person [crud-state]
  [:button {:style button-style}
   "delete"])

(defn crud-ui [crud-state]
  [:div {:style {:display         "flex"
                 :flex-direction  "column"
                 :justify-content "space-between"}}
   [:div {:padding "1em"}
    [filter-prefix crud-state]
    [people-ui]]
   [:div {:style {:display "flex"
                  :padding "1em"}}
    [create-person crud-state]
    [update-person crud-state]
    [delete-person crud-state]]])
