(ns sguis.workspaces.crud
  (:require [clojure.string :as str]
            [reagent.core :as r]))

(def *crud
  (r/atom {:next-id      0
           :filter-prefix ""
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
               :value (when name-insertion
                        name-insertion)
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
         :keys        [current-id
                       filter-prefix]} @crud-state
        data                        (vals by-id)
        filtered-data (filter #(str/starts-with?
                                (str (:surname %)
                                     ","
                                     (:name %))
                                filter-prefix) data)]
    [:ul {:style {:list-style-type "none"
                  :padding         0
                  :margin          0}}
     (for [{:keys [id
                   name
                   surname]} filtered-data
           :let [current? (= current-id id)
                 show-name (str surname ", " name)]]
       [:li {:style    (if current?
                         selection-style
                         {})
             :on-click #(swap! crud-state assoc
                               :name-insertion name
                               :surname-insertion surname
                               :current-id id)}
        [:a {:style (if current?
                      (assoc a-style :color "white")
                      a-style)
             :href  "#"}
         show-name]])]))

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

(defn clear-input-fields! [crud-state]
  (swap! crud-state
         dissoc
         :name-insertion
         :surname-insertion
         :current-id))

(defn create-person [crud-state]
  (let [{:keys [name-insertion
                surname-insertion
                next-id]} @crud-state
        empty-inputs? (and (empty? name-insertion)
                           (empty? surname-insertion))]
    (letfn [(create-person! [crud-state]
              (swap! crud-state
                     assoc-in
                     [:person/by-id next-id]
                     {:id      next-id
                      :name    name-insertion
                      :surname surname-insertion}))
            (increment-id! [crud-state]
              (swap! crud-state
                     update
                     :next-id
                     inc))]
      [:button {:style    button-style
                :on-click #(when-not empty-inputs?
                             (create-person! crud-state)
                             (clear-input-fields! crud-state)
                             (increment-id! crud-state))}
       "create"])))

(defn update-person [crud-state]
  (let [{:keys [current-id
                name-insertion
                surname-insertion]} @crud-state]
    (letfn [(update-selection! []
              (swap! crud-state
                     update-in
                     [:person/by-id current-id]
                     #(assoc %
                             :name name-insertion
                             :surname surname-insertion)))]
      [:button {:style    button-style
                :disabled (not current-id)
                :on-click (fn [_]
                            (update-selection! crud-state)
                            (clear-input-fields! crud-state))}
       "update"])))

(defn delete-person [crud-state]
  (let [{:keys [current-id]} @crud-state]
    (letfn [(delete-selection! [crud-state]
              (swap! crud-state
                     update
                     :person/by-id
                     dissoc
                     current-id))]
      [:button {:style    button-style
                :disabled (not current-id)
                :on-click #(do (delete-selection! crud-state)
                               (clear-input-fields! crud-state))}
       "delete"])))

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
    [delete-person crud-state]]])
