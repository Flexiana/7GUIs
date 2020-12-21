(ns sguis.workspaces.crud
  (:require [reagent.core :as r]))

(defn crud-ui []
  [:div {:style {:display "flex"
                 :flex-direction "column"
                 :justify-content "space-between"}}
   [:div {:style {:margin-bottom "50px"}}
    [:lable "Filter pefix"]
    [:input {:type "text"}]]
   [:div {:style {:display "flex"
                  :justify-content "space-between"
                  :width "600px"
                  :margin-bottom "50px"}}
    [:div {:style {:width "45%"
                   :height "200px"
                   :border "1px solid gray"}}
     "name lastname"]
    [:div {:style {:display "flex"
                   :flex-direction "column"
                   :width "45%"}}
     [:span
      [:label "Name:"]
      [:input {:type "text"}]]
     [:span
      [:label "Lastname:"]
      [:input {:type "text"}]]]]
   [:div {:style {:display "flex"}}
    [:button {:style {:margin-right "20px"
                      :cursor "pointer"}}
     "create"]
    [:button {:style {:margin-right "20px"
                      :cursor "pointer"}}
     "update"]
    [:button{:style {:margin-right "20px"
                     :cursor "pointer"}}
     "delete"]]])
