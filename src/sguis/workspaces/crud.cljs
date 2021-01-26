(ns sguis.workspaces.crud
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sguis.workspaces.utils :as u]))

(def crud-start
  {:next-id           0
   :filter-prefix     ""
   :person/by-id      {}
   :current-id        nil
   :name-insertion    nil
   :surname-insertion nil})

(defn change-filter-prefix! [*state e]
  (swap! *state
         assoc
         :filter-prefix
         (.. e -target -value)))

(defn filter-field [change-filter-prefix!]
  [:div.control.is-expanded
   [:label "Filter prefix: "]
   [:div.field.is-fullwidth
    [:input.input
     {:type        "text"
      :data-testid "filter"
      :on-change   change-filter-prefix!}]]])

(defn text-field [id value label on-change]
  [:label label
   [:div.field
    [:input.input {:type        "text"
                   :data-testid label
                   :value       (when value value)
                   :on-change   (partial on-change id)}]]])

(defn insert-value! [*state id event]
  (swap! *state assoc id (-> event .-target .-value)))

(defn insert-panel [{:keys [name-insertion surname-insertion]} insert-value!]
  [:div.tile.is-child
   [text-field :name-insertion name-insertion "Name: " insert-value!]
   [text-field :surname-insertion surname-insertion "Surname: " insert-value!]])

(defn matching-name? [filter-prefix {:keys [surname name]}]
  (str/includes? (str/lower-case (str surname name)) (str/lower-case filter-prefix)))

(defn person-row [{:keys [current-id]} select-person! {:keys [name surname id] :as person}]
  (let [show-name (str surname ", " name)]
    ^{:key id}
     [:li.input.panel-block
      {:class    (u/classes (when (= current-id id) :is-danger))
       :on-click (partial select-person! person)}
      show-name]))

(defn person-list [{:person/keys [by-id]
                    :keys        [filter-prefix]
                    :as          state} select-person!]
  [:ul.panel {:data-testid "person-list"}
   (->> by-id
        vals
        (filter (partial matching-name? filter-prefix))
        (map (partial person-row state select-person!)))])

(defn select-person! [*state {:keys [name surname id]}]
  (swap! *state assoc
         :name-insertion name
         :surname-insertion surname
         :current-id id))

(defn people-panel [state {:keys [change-filter-prefix!
                                  select-value!
                                  insert-value!]}]
  [:div.tile.is-parent
   [:div.columns
    [:div.column.is-half
     [:div.field
      [filter-field change-filter-prefix!]
      [person-list state select-value!]]]
    [:div.column.is-half
     [:div.field
      [insert-panel state insert-value!]]]]])

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

(defn create-person! [*state {:keys [name-insertion surname-insertion next-id]}]
  (when-not (and (empty? name-insertion)
                 (empty? surname-insertion))
    (swap! *state
           assoc-in
           [:person/by-id next-id]
           {:id      next-id
            :name    name-insertion
            :surname surname-insertion})
    (clear-input-fields! *state)
    (increment-id! *state)))

(defn update-person! [*state]
  (let [{:keys [name-insertion
                surname-insertion
                current-id]} @*state]
    (swap! *state
           update-in
           [:person/by-id current-id]
           #(assoc %
                   :name name-insertion
                   :surname surname-insertion))
    (clear-input-fields! *state)))

(defn delete-person! [*state]
  (let [{:keys [current-id]} @*state]
    (swap! *state
           update
           :person/by-id
           dissoc
           current-id)
    (clear-input-fields! *state)))

(defn crud-button [{:keys [current-id] :as state} btn-type action]
  [:button.button {:data-testid (name btn-type)
                   :class (u/classes (case btn-type
                                       :create :is-primary
                                       :update :is-success
                                       :delete :is-danger))
                   :disabled (if (or (= btn-type :update)
                                     (= btn-type :delete))
                               (not current-id)
                               false)
                   :on-click (if (= btn-type :create)
                               (partial action state)
                               action)}
   (name btn-type)])

(defn crud-ui
  ([]
   (r/with-let [*crud (r/atom crud-start)]
     [crud-ui *crud]))
  ([*state]
   [:div.panel.is-primary
    {:style {:min-width "24em"}}
    [:div.panel-heading "CRUD"]
    [:div.panel-block.is-block
     [people-panel @*state {:change-filter-prefix! (partial change-filter-prefix! *state)
                            :select-value!         (partial select-person! *state)
                            :insert-value!         (partial insert-value! *state)}]

     [crud-button @*state :create (partial create-person! *state)]
     [crud-button @*state :update (partial update-person! *state)]
     [crud-button @*state :delete (partial delete-person! *state)]]]))
