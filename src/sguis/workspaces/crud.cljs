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

(def button-style
  {:margin-right  "20px"
   :cursor        "pointer"
   :border-radius "50px"})

(defn filter-prefix [*state]
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


(defn selection-as-input-fields! [*state {:keys [name surname id]}]
  (swap! *state assoc
         :name-insertion name
         :surname-insertion surname
         :current-id id))


(defn list-ui [*state {:keys [id
                              name
                              surname]
                       :as   selection}]
  (let [{:keys [current-id]} @*state
        selected?            (= current-id id)
        show-name            (str surname ", " name)]
    ^{:key show-name}
    [:li {:style    (if selected?
                      (assoc read-style
                             :color "white"
                             :background-color "blue")
                      read-style)
          :on-click #(selection-as-input-fields! *state selection)}
     show-name]))

(defn read-ui [*state]
  (let [{:person/keys [by-id]
         :keys        [filter-prefix]} @*state]
    [:ul {:style {:list-style-type "none"
                  :padding         0
                  :margin          0}}
     (doall
      (->> by-id
           vals
           (filter (partial matching-name? filter-prefix))
           (map (partial list-ui *state))))]))


(defn people-ui [*state]
  [:div {:style {:display         "flex"
                 :justify-content "space-between"}}
   [:div {:style {:width  "100%"
                  :height "100%"
                  :border "1px solid gray"}}
    [read-ui *state]]
   [:div {:style {:display        "flex"
                  :padding        "1em"
                  :flex-direction "column"}}
    [insert-panel @*state (partial insert-value! *state)]]])

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

(defn create-person! [*state]
  (let [{:keys [name-insertion
                surname-insertion
                next-id]} @*state]
    (swap! *state
           assoc-in
           [:person/by-id next-id]
           {:id      next-id
            :name    name-insertion
            :surname surname-insertion})))

(defn create-person [*state]
  (let [{:keys [name-insertion
                surname-insertion]} @*state
        empty-inputs?               (and (empty? name-insertion)
                                         (empty? surname-insertion))]
    [:button {:style    button-style
              :on-click #(when-not empty-inputs?
                           (create-person! *state)
                           (clear-input-fields! *state)
                           (increment-id! *state))}
     "create"]))

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

(defn update-person [*state]
  (let [{:keys [current-id]} @*state]
    [:button {:style    button-style
              :disabled (not current-id)
              :on-click #(do (update-selection! *state)
                             (clear-input-fields! *state))}
     "update"]))

(defn delete-selection! [*state]
  (let [{:keys [current-id]} @*state]
    (swap! *state
           update
           :person/by-id
           dissoc
           current-id)))

(defn delete-person [*state]
  (let [{:keys [current-id]} @*state]
    [:button {:style    button-style
              :disabled (not current-id)
              :on-click #(do (delete-selection! *state)
                             (clear-input-fields! *state))}
     "delete"]))

(defn crud-ui [*state]
  [:div {:style {:display         "flex"
                 :flex-direction  "column"
                 :justify-content "space-between"}}
   [:div {:padding "1em"}
    [filter-prefix *state]
    [people-ui *state]]
   [:div {:style {:display "flex"
                  :padding "1em"}}
    [create-person *state]
    [update-person *state]
    [delete-person *state]]])
