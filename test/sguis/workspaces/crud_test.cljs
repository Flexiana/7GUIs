(ns sguis.workspaces.crud-test
  (:require [sguis.workspaces.crud :refer [crud-ui crud-start]]
            [cljs.test :as t
             :include-macros true
             :refer [are is testing]]
            [nubank.workspaces.core :as ws]
            [sguis.workspaces.test-utils :as u]
            [reagent.core :as r]))

(defn texts-on-field [field]
  (mapv #(.-innerText %) (.-children field)))

(def selectors-map
  {:filter-field  #(.getByTestId % "filter")
   :name-field    #(.getByTestId % "Name:")
   :surname-field #(.getByTestId % "Surname:")
   :create-button #(.getByText % "create")
   :update-button #(.getByText % "update")
   :delete-button #(.getByText % "delete")
   :person-list   #(.getByTestId % "person-list")})

(ws/deftest crud-tests
  (let [{:keys [filter-field
                name-field
                surname-field
                create-button
                update-button
                delete-button
                person-list]} selectors-map
        first-person-list     #(-> % person-list .-children first)
        *crud                 (r/atom crud-start)]
    (u/with-mounted-component
      [crud-ui *crud]
      (fn [comp]

        ;; TODO discuss if we need this render
        (testing "Initial render"
          (is (empty? (.-value (filter-field comp))))
          (is (empty? (.-value (name-field comp))))
          (is (empty? (.-value (surname-field comp))))
          (is (= js/HTMLButtonElement (type (create-button comp))))
          (is (= js/HTMLButtonElement (type (update-button comp))))
          (is (= js/HTMLButtonElement (type (delete-button comp))))
          (is (false? (.-disabled (create-button comp))))
          (is (.-disabled (update-button comp)))
          (is (.-disabled (delete-button comp)))
          (is (= js/HTMLUListElement (type (person-list comp)))))

        (testing "Create person"
          (u/input-element! (name-field comp) "John")
          (u/input-element! (surname-field comp) "Doe")
          (is (= "John" (.-value (name-field comp))))
          (is (= "Doe" (.-value (surname-field comp))))
          (are [expected actual] (= expected actual)
            "John" (:name-insertion @*crud)
            "Doe"  (:surname-insertion @*crud))
          (u/click-element! (create-button comp))
          (are [expected actual] (= expected actual)
            nil                                       (:name-insertion @*crud)
            nil                                       (:surname-insertion @*crud)
            {0 {:id 0, :name "John", :surname "Doe"}} (:person/by-id @*crud))
          (is (= (texts-on-field (person-list comp)) ["Doe, John"])))

        (testing "Update person"
          (u/click-element! (first-person-list comp))
          (are [expected actual] (= expected actual)
            "John" (:name-insertion @*crud)
            "Doe"  (:surname-insertion @*crud))
          (is (= "John" (.-value (name-field comp))))
          (is (= "Doe" (.-value (surname-field comp))))
          (u/input-element! (name-field comp) "Jane")
          (u/click-element! (update-button comp))
          (are [expected actual] (= expected actual)
            nil                                       (:name-insertion @*crud)
            nil                                       (:surname-insertion @*crud)
            {0 {:id 0, :name "Jane", :surname "Doe"}} (:person/by-id @*crud))
          (is (= ["Doe, Jane"]
                 (texts-on-field (person-list comp)))))

        (testing "Filtering"
          (u/input-element! (name-field comp) "John")
          (u/input-element! (surname-field comp) "Foe")
          (u/click-element! (create-button comp))
          (u/input-element! (filter-field comp) "F")
          (are [expected actual] (= expected actual)
            "F"                                       (:filter-prefix @*crud)
            {0 {:id 0, :name "Jane", :surname "Doe"}
             1 {:id 1, :name "John", :surname "Foe"}} (:person/by-id @*crud))
          (is (= ["Foe, John"] (texts-on-field (person-list comp)))))

        (testing "Delete person"
          (u/click-element! (first-person-list comp))
          (u/click-element! (delete-button comp))
          (are [expected actual] (= expected actual)
            {0 {:id 0, :name "Jane", :surname "Doe"}} (:person/by-id @*crud))
          (u/input-element! (filter-field comp) "")
          (is (= ["Doe, Jane"]
                 (texts-on-field (person-list comp)))))))))
