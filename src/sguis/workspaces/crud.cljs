(ns sguis.workspaces.crud
  (:require [reagent.core :as r]))

(def *crud
  (r/atom {:next-id      0
           :person/by-id {}}))

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
  (let [{:keys [name-insertion
                surname-insertion]} @crud-state]
    [:div {:padding "1em"}
     [:label "Name: "
      [:input {:type      "text"
               :value (when name-insertion name-insertion)
               :on-change #(swap! crud-state
                                  assoc
                                  :name-insertion
                                  (.. % -target -value))}]]
     [:label "Surname: "
      [:input {:type      "text"
               :value (when surname-insertion
                        surname-insertion)
               :on-change #(swap! crud-state
                                  assoc
                                  :surname-insertion
                                  (.. % -target -value))}]]]))

(def a-style
  {:text-decoration "none"
   :color           "black"})

(def selection-style
  {:background-color "blue"})

(defn read-ui [crud-state]
  (let [{:person/keys [by-id]
         :keys        [current-id]} @crud-state
        data                             (vals by-id)]
    [:ul {:style {:list-style-type "none"
                  :padding         0
                  :margin          0}}
     (for [{:keys [id
                   name
                   surname]} data]
       [:li {:style    (if (= current-id id)
                         selection-style
                         {})
             :on-focus #(swap! crud-state assoc
                               :name-insertion name
                               :surname-insertion surname
                               :current-id id)}
        [:a {:style (if  (= current-id id)
                      (assoc a-style :color "white")
                      a-style)
             :href  "#"}
         (str surname ", " name)]])]))

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
                           (swap! crud-state assoc-in [:person/by-id next-id] {:id      next-id
                                                                               :name    name-insertion
                                                                               :surname surname-insertion})
                           (swap! crud-state update :next-id inc))}
     "create"]))

(defn update-person [crud-state]
  (let [{:keys [current-id
                name-insertion
                surname-insertion]} @crud-state]
    [:button {:style    button-style
              :disabled (not current-id)
              :on-click (fn [_]
                          (swap! crud-state
                                 update-in [:person/by-id current-id]
                                 #(assoc % :name name-insertion
                                         :surname surname-insertion))
                          (swap! crud-state dissoc :current-id))}
     "update"]))

(defn delete-person [crud-state]
  (let [{:keys [current-id]} @crud-state]
    [:button {:style    button-style
              :disabled (not current-id)
              :on-click #(do (swap! crud-state update :person/by-id dissoc current-id)
                             (swap! crud-state dissoc :current-id))}
     "delete"]))

(defn crud-ui [crud-state]
  (r/create-class
   {:component-did-mount (do (swap! crud-state
                                    assoc-in
                                    [:person/by-id -1]
                                    {:id      -1
                                     :name    "a"
                                     :surname "z"})
                             (swap! crud-state
                                    assoc-in
                                    [:person/by-id -2]
                                    {:id      -2
                                     :name    "a"
                                     :surname "zz"}))
    :reagent-render      (fn []
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
                             [delete-person crud-state]]])}))
