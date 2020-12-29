(ns sguis.workspaces.crud
  (:require [clojure.string :as str]
            [reagent.core :as r]))

(def *crud
  (r/atom {:next-id           0
           :filter-prefix     ""
           :person/by-id      {}
           :current-id        nil
           :name-insertion    nil
           :surname-insertion nil}))

(defn filter-field [*state]
  [:div {:padding "1em"}
   [:label "Filter prefix: "]
   [:input {:type      "text"
            :on-change #(swap! *state
                               assoc
                               :filter-prefix
                               (.. % -target -value))}]])

(defn text-field [id value label on-change]
  [:label label
   [:input {:type      "text"
            :value     (when value value)
            :on-change (partial on-change id)}]])

(defn insert-value! [*state id event]
  (swap! *state assoc id (-> event .-target .-value)))

(defn insert-panel [{:keys [name-insertion surname-insertion]} insert-value!]
  [:div {:padding "1em"}
   [text-field :name-insertion name-insertion "Name: " insert-value!]
   [text-field :surname-insertion surname-insertion "Surname: " insert-value!]])

(def read-style
  {:text-decoration "none"
   :color           "black"})

(defn matching-name? [filter-prefix {:keys [surname name]}]
  (str/starts-with? (str surname "," name) filter-prefix))

(defn person-row [{:keys [current-id]} select-person! {:keys [name surname id] :as person}]
  (let [show-name (str surname ", " name)]
    ^{:key id} [:li {:style    (if (= current-id id)
                                 (assoc read-style
                                        :color "white"
                                        :background-color "blue")
                                 read-style)
                     :on-click (partial select-person! person)}
                show-name]))

(defn person-list [{:person/keys [by-id]
                    :keys        [filter-prefix]
                    :as          state} select-person!]
  [:ul {:style {:list-style-type "none", :padding 0, :margin 0}}
   (->> by-id
        vals
        (filter (partial matching-name? filter-prefix))
        (map (partial person-row state select-person!)))])

(defn select-person! [*state {:keys [name surname id]}]
  (swap! *state assoc
         :name-insertion name
         :surname-insertion surname
         :current-id id))

(defn people-panel [*state]
  [:div {:style {:display "flex" :justify-content "space-between"}}
   [:div {:style {:width "100%" :height "100%" :border "1px solid gray"}}
    [person-list @*state (partial select-person! *state)]]
   [:div {:style {:display "flex" :padding "1em" :flex-direction "column"}}
    [insert-panel @*state (partial insert-value! *state)]]])

(def button-style
  {:margin-right  "20px"
   :cursor        "pointer"
   :border-radius "50px"})

(defn clear-input-fields! [*state]
  (swap! *state
         dissoc
         :name-insertion
         :surname-insertion
         :current-id))

(defn increment-id! [*state]
  (swap! *state
         update
         :next-id
         inc))

(defn insert-person! [*state]
  (let [{:keys [name-insertion
                surname-insertion
                next-id]} @*state]
    (swap! *state
           assoc-in
           [:person/by-id next-id]
           {:id      next-id
            :name    name-insertion
            :surname surname-insertion})))

(defn create-person! [*state empty-inputs?]
  (when-not empty-inputs?
    (insert-person! *state)
    (clear-input-fields! *state)
    (increment-id! *state)))

(defn empty-inputs? [name-insertion surname-insertion]
  (and (empty? name-insertion)
       (empty? surname-insertion)))

(defn create-button [{:keys [name-insertion
                             surname-insertion]} create-person!]
  [:button {:style    button-style
            :on-click #(create-person! (empty-inputs? name-insertion surname-insertion))}
   "create"])

(defn update-selection! [*state]
  (let [{:keys [name-insertion
                surname-insertion
                current-id]} @*state]
    (swap! *state
           update-in
           [:person/by-id current-id]
           #(assoc %
                   :name name-insertion
                   :surname surname-insertion))))

(defn update-person! [*state]
  (update-selection! *state)
  (clear-input-fields! *state))

(defn update-button [{:keys [current-id]} update-person!]
  [:button {:style    button-style
            :disabled (not current-id)
            :on-click update-person!}
   "update"])

(defn delete-selection! [*state]
  (let [{:keys [current-id]} @*state]
    (swap! *state
           update
           :person/by-id
           dissoc
           current-id)))

(defn delete-person! [*state]
  (delete-selection! *state)
  (clear-input-fields! *state))

(defn delete-button [{:keys [current-id]} delete-person!]
  [:button {:style    button-style
            :disabled (not current-id)
            :on-click delete-person!}
   "delete"])

(defn crud-ui [*state]
  [:div {:style {:display         "flex"
                 :flex-direction  "column"
                 :justify-content "space-between"}}
   [:div {:padding "1em"}
    [filter-field *state]
    [people-panel *state]
    [create-button @*state (partial create-person! *state)]
    [update-button @*state (partial update-person! *state)]
    [delete-button @*state (partial delete-person! *state)]]])
