(ns sguis.workspaces.crud
  (:require [reagent.core :as r]
            [cljs.pprint :as pp]))

(def *crud
  (r/atom {:next-id 0
           :data    []}))

(def button-style
  {:margin-right  "20px"
   :cursor        "pointer"
   :border-radius "50px"})

(defn filter-prefix [crud-state]
  [:div {:padding "1em"}
   [:label "Filter prefix: "]
   [:input {:type      "text"
            :on-change #(swap! crud-state
                               assoc
                               :filter-prefix
                               (.. % -target -value))}]])

(defn insert-ui [crud-state]
  [:div {:padding "1em"}
   [:label "Name: "
    [:input {:type      "text"
             :on-change #(swap! crud-state
                                assoc
                                :name-insertion
                                (.. % -target -value))}]]
   [:label "Surname: "
    [:input {:type      "text"
             :on-change #(swap! crud-state
                                assoc
                                :surname-insertion
                                (.. % -target -value))}]]])

;; todo, change to selector
(defn read-ui [crud-state]
  (let [{:keys [data]} @crud-state]
    [:<> (for [{:keys [name
                       surname
                       id]} data]
           [:div {:id id}
            [:span surname]
            [:span ", "]
            [:span name]])]))

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
  (let [{:keys [name-insertion
                surname-insertion
                next-id]} @crud-state]
    [:button {:style    button-style
              :on-click #(when-not (and (empty? name-insertion)
                                        (empty? surname-insertion))
                           (swap! crud-state update :data conj {:id      next-id
                                                                :name    name-insertion
                                                                :surname surname-insertion})
                           (swap! crud-state update :next-id inc))}
     "create"]))

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
    [people-ui crud-state]]
   [:div {:style {:display "flex"
                  :padding "1em"}}
    [create-person crud-state]
    [update-person crud-state]
    [delete-person crud-state]]
   [:pre (with-out-str (pp/pprint @crud-state))]])
