(ns sguis.workspaces.crud
  "7GUIs Crud UI"
  (:require
    [clojure.string :as str]
    [reagent.core :as r]
    [sguis.workspaces.utils :as u]))

(def crud-start
  "Init values"
  {:next-id           0
   :filter-field      ""
   :person/by-id      {}
   :current-id        nil
   :name-insertion    nil
   :surname-insertion nil})

(defn change-filter!
  "Update filter in application state"
  [*state e]
  (swap! *state
    assoc
    :filter-field
    (.. e -target -value)))

(defn filter-field
  "Input for filtering results"
  [change-filter-prefix!]
  [:div.control.is-expanded
   [:label "Filter prefix: "]
   [:div.field.is-fullwidth
    [:input.input
     {:type        "text"
      :data-testid "filter"
      :on-change   change-filter-prefix!}]]])

(defn text-field
  "Common used text field"
  [id value label on-change]
  [:label label
   [:input.input {:type        "text"
                  :data-testid label
                  :value       (when value value)
                  :on-change   (partial on-change id)}]])

(defn insert-value!
  "Text field's onchange function"
  [*state id event]
  (swap! *state assoc id (-> event .-target .-value)))

(defn insert-panel
  "Input fields to get name and surname parameters"
  [{:keys [name-insertion surname-insertion]} insert-value!]
  [:div.tile.is-child
   [text-field :name-insertion name-insertion "Name: " insert-value!]
   [text-field :surname-insertion surname-insertion "Surname: " insert-value!]])

(defn matching-name?
  "Predict name matching for filtered output"
  [filter-field {:keys [surname name]}]
  (str/includes? (str/lower-case (str surname name)) (str/lower-case filter-field)))

(defn person-row
  "Visual representation of a person"
  [{:keys [current-id]} select-person! {:keys [name surname id] :as person}]
  (let [show-name (str surname ", " name)]
    ^{:key id}
    [:li.input.panel-block
     {:class    (u/classes (when (= current-id id) :is-danger))
      :on-click (partial select-person! person)}
     show-name]))

(defn person-list
  "Visual representation of persons"
  [{:person/keys [by-id]
    :keys        [filter-field]
    :as          state} select-person!]
  [:ul.panel {:data-testid "person-list"}
   (->> by-id
        vals
        (filter (partial matching-name? filter-field))
        (map (partial person-row state select-person!)))])

(defn select-person!
  "Action, when a person is selected"
  [*state {:keys [name surname id]}]
  (swap! *state assoc
    :name-insertion name
    :surname-insertion surname
    :current-id id))

(defn clear-input-fields
  "Reset input fields to it's original states"
  [state]
  (dissoc state
          :name-insertion
          :surname-insertion
          :current-id))

(defn increment-id
  "Prepares next ID on person creation"
  [state]
  (update state
    :next-id
    inc))

(defn create-person
  "Stores a newly created person"
  [{:keys [name-insertion surname-insertion next-id]}
   state]
  (assoc-in state
    [:person/by-id next-id]
    {:id      next-id
     :name    name-insertion
     :surname surname-insertion}))

(defn create-person!
  "Try to do a person creation"
  [*state {:keys [name-insertion surname-insertion] :as state}]
  (when-not (and (empty? name-insertion)
                 (empty? surname-insertion))
    (swap! *state (comp (partial create-person state)
                        clear-input-fields
                        increment-id))))

(defn update-person
  "Stores an updated person"
  [{:keys [name-insertion surname-insertion current-id]}
   state]
  (update-in state
    [:person/by-id current-id]
    #(assoc %
       :name name-insertion
       :surname surname-insertion)))

(defn update-person!
  "Updates a person"
  [*state state]
  (swap! *state (comp (partial update-person state)
                      clear-input-fields)))

(defn delete-person
  "Removes a person from store"
  [{:keys [current-id]}
   state]
  (update state
    :person/by-id
    dissoc
    current-id))

(defn delete-person!
  "Removes a person"
  [*state state]
  (swap! *state (comp (partial delete-person state)
                      clear-input-fields)))

(defn crud-button
  "Visual representation of the CRUD operations"
  [{:keys [current-id] :as state} btn-type action]
  [:div.column
   [:button.button {:class    (u/classes (case btn-type
                                           :create :is-primary
                                           :update :is-success
                                           :delete :is-danger))
                    :disabled (if (or (= btn-type :update)
                                      (= btn-type :delete))
                                (not current-id)
                                false)
                    :on-click (partial action state)}
    (let [[f & rest] (name btn-type)]
      (str (str/upper-case f)
           (str/join rest)))]])

(defn people-panel
  "UI representation of stored peoples, with applied filter"
  [state {:keys [change-filter-prefix!
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

(defn crud-ui
  "UI of CRUD application"
  ([]
   (r/with-let [*crud (r/atom crud-start)]
     [crud-ui *crud]))
  ([*state]
   [:div.panel.is-primary
    {:style {:min-width "24em"}}
    [:div.panel-heading "CRUD"]
    [:div.panel-block.is-block
     [people-panel @*state {:change-filter-prefix! (partial change-filter! *state)
                            :select-value!         (partial select-person! *state)
                            :insert-value!         (partial insert-value! *state)}]

     [:div.columns
      [crud-button @*state :create (partial create-person! *state)]
      [crud-button @*state :update (partial update-person! *state)]
      [crud-button @*state :delete (partial delete-person! *state)]]]]))
