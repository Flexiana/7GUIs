(ns sguis.workspaces.crud
  (:require [reagent.core :as r]))

(def *crud
  (r/atom {}))

(defn read-component [crud-state]
  [:span
   [:label "Name:"]
   [:input {:type "text"}]]
  [:span
   [:label "Lastname:"]
   [:input {:type "text"}]])

(defn filter-prefix [crud-state]
  [:div {:style {:margin-bottom "50px"}}
   [:label "Filter prefix"]
   [:input {:type "text"}]])

(defn create-person [crud-state]
  [:button {:style {:margin-right "20px"
                    :cursor       "pointer"}}
   "create"])

(defn update-person [crud-state]
  [:button {:style {:margin-right "20px"
                    :cursor       "pointer"}}
   "update"])

(defn delete-person [crud-state]
  [:button{:style {:margin-right "20px"
                   :cursor       "pointer"}}
   "delete"])

(defn people-ui [crud-state]
  [:div {:style {:display         "flex"
                 :justify-content "space-between"
                 :width           "600px"
                 :margin-bottom   "50px"}}
   [:div {:style {:width  "45%"
                  :height "200px"
                  :border "1px solid gray"}}
    "name lastname"]
   [:div {:style {:display        "flex"
                  :flex-direction "column"
                  :width          "45%"}}
    [read-component crud-state]]])

(defn crud-ui [crud-state]
  [:div {:style {:display         "flex"
                 :flex-direction  "column"
                 :justify-content "space-between"}}
   [filter-prefix crud-state]
   [people-ui]
   [:div {:style {:display "flex"}}
    [create-person crud-state]
    [update-person crud-state]
    [delete-person crud-state]]])
