(ns sguis.workspaces.crud-test
  (:require [sguis.workspaces.crud :refer [crud-ui crud-start]]
            [cljs.test :as t
             :include-macros true
             :refer [are is testing]]
            [nubank.workspaces.core :as ws]
            [sguis.workspaces.test-utils :as u]
            [reagent.core :as r]))

(ws/deftest crud-tests
  (let [filter-field  #(.getByTestId % "filter")
        name-field    #(.getByTestId % "Name:")
        surname-field #(.getByTestId % "Surname:")
        create-button #(.getByText % "create")
        update-button #(.getByText % "update")
        delete-button #(.getByText % "delete")
        person-list   #(.getByTestId % "person-list")
        *crud         (r/atom crud-start)]
    (u/with-mounted-component
      [crud-ui *crud]
      (fn [comp]
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
          (reset! *crud crud-start)
          (u/input-element! (name-field comp) "John")
          (is (= "John" (.-value (name-field comp))))
          (u/input-element! (surname-field comp) "Doe")
          (is (= "Doe" (.-value (surname-field comp))))
          (u/click-element! (create-button comp))
          (is (= (mapv #(.-innerHTML %) (.-children (person-list comp))) '("Doe, John"))))

        (testing "Update person"
          (reset! *crud crud-start)
          (u/input-element! (name-field comp) "John")
          (u/input-element! (surname-field comp) "Doe")
          (u/click-element! (create-button comp))
          (is (empty? (.-value (name-field comp))))
          (is (empty? (.-value (surname-field comp))))
          (u/click-element! (-> (person-list comp) .-children first))
          (is (= "John" (.-value (name-field comp))))
          (is (= "Doe" (.-value (surname-field comp))))
          (u/input-element! (name-field comp) "Jane")
          (u/click-element! (update-button comp))
          (is (= (mapv #(.-innerHTML %) (.-children (person-list comp))) '("Doe, Jane"))))

        (testing "Filtering"
          (reset! *crud crud-start)
          (u/input-element! (name-field comp) "John")
          (u/input-element! (surname-field comp) "Doe")
          (u/click-element! (create-button comp))
          (u/input-element! (name-field comp) "John")
          (u/input-element! (surname-field comp) "Foe")
          (u/click-element! (create-button comp))
          (u/input-element! (filter-field comp) "F")
          (is (= (mapv #(.-innerHTML %) (.-children (person-list comp))) '("Foe, John"))))

        (testing "Delete person"
          (reset! *crud crud-start)
          (u/input-element! (name-field comp) "John")
          (u/input-element! (surname-field comp) "Doe")
          (u/click-element! (create-button comp))
          (u/input-element! (name-field comp) "John")
          (u/input-element! (surname-field comp) "Foe")
          (u/click-element! (create-button comp))
          (u/click-element! (-> (person-list comp) .-children first))
          (u/click-element! (delete-button comp))
          (is (= (mapv #(.-innerHTML %) (.-children (person-list comp))) '("Foe, John"))))

        (testing "State"
          (reset! *crud crud-start)
          (u/input-element! (name-field comp) "John")
          (u/input-element! (surname-field comp) "Doe")
          (are [expected actual] (= expected actual)
            "John" (:name-insertion @*crud)
            "Doe"  (:surname-insertion @*crud))
          (u/click-element! (create-button comp))
          (are [expected actual] (= expected actual)
            nil                                       (:name-insertion @*crud)
            nil                                       (:surname-insertion @*crud)
            {0 {:id 0, :name "John", :surname "Doe"}} (:person/by-id @*crud))
          (u/click-element! (-> (person-list comp) .-children first))
          (are [expected actual] (= expected actual)
            "John" (:name-insertion @*crud)
            "Doe"  (:surname-insertion @*crud))
          (u/input-element! (surname-field comp) "Foe")
          (u/click-element! (update-button comp))
          (u/click-element! (-> (person-list comp) .-children first))
          (are [expected actual] (= expected actual)
            "John" (:name-insertion @*crud)
            "Foe"  (:surname-insertion @*crud))
          (u/click-element! (delete-button comp))
          (are [expected actual] (= expected actual)
            nil (:name-insertion @*crud)
            nil (:surname-insertion @*crud)
            {}  (:person/by-id @*crud)))))))
