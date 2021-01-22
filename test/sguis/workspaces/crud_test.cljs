(ns sguis.workspaces.crud-test
  (:require [sguis.workspaces.crud :refer [crud-ui *crud]]
            [cljs.test :as t
             :include-macros true
             :refer [are is testing]]
            [nubank.workspaces.core :as ws]
            [sguis.workspaces.test-utils :as u]))

(defn reset-state! [*crud]
  (reset! *crud {:next-id           0
                 :filter-prefix     ""
                 :person/by-id      {}
                 :current-id        nil
                 :name-insertion    nil
                 :surname-insertion nil}))

(ws/deftest
  crud-tests
  (let [filter-field #(.getByTestId % "filter")
        name-field #(.getByTestId % "Name:")
        surname-field #(.getByTestId % "Surname:")
        create-button #(.getByText % "create")
        update-button #(.getByText % "update")
        delete-button #(.getByText % "delete")
        person-list #(.getByTestId % "person-list")]
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
          (reset-state! *crud)
          (u/input-element! (name-field comp) {:target {:value "John"}})
          (is (= "John" (.-value (name-field comp))))
          (u/input-element! (surname-field comp) {:target {:value "Doe"}})
          (is (= "Doe" (.-value (surname-field comp))))
          (u/click-element! (create-button comp))
          (is (= (mapv #(.-innerHTML %) (.-children (person-list comp))) '("Doe, John"))))
        (testing "Update person"
          (reset-state! *crud)
          (u/input-element! (name-field comp) {:target {:value "John"}})
          (u/input-element! (surname-field comp) {:target {:value "Doe"}})
          (u/click-element! (create-button comp))
          (is (empty? (.-value (name-field comp))))
          (is (empty? (.-value (surname-field comp))))
          (u/click-element! (-> (person-list comp) .-children first))
          (is (= "John" (.-value (name-field comp))))
          (is (= "Doe" (.-value (surname-field comp))))
          (u/input-element! (name-field comp) {:target {:value "Jane"}})
          (u/click-element! (update-button comp))
          (is (= (mapv #(.-innerHTML %) (.-children (person-list comp))) '("Doe, Jane"))))
        (testing "Filtering"
          (reset-state! *crud)
          (u/input-element! (name-field comp) {:target {:value "John"}})
          (u/input-element! (surname-field comp) {:target {:value "Doe"}})
          (u/click-element! (create-button comp))
          (u/input-element! (name-field comp) {:target {:value "John"}})
          (u/input-element! (surname-field comp) {:target {:value "Foe"}})
          (u/click-element! (create-button comp))
          (u/input-element! (filter-field comp) {:target {:value "F"}})
          (is (= (mapv #(.-innerHTML %) (.-children (person-list comp))) '("Foe, John"))))
        (testing "Delete person"
          (reset-state! *crud)
          (u/input-element! (name-field comp) {:target {:value "John"}})
          (u/input-element! (surname-field comp) {:target {:value "Doe"}})
          (u/click-element! (create-button comp))
          (u/input-element! (name-field comp) {:target {:value "John"}})
          (u/input-element! (surname-field comp) {:target {:value "Foe"}})
          (u/click-element! (create-button comp))
          (u/click-element! (-> (person-list comp) .-children first))
          (u/click-element! (delete-button comp))
          (is (= (mapv #(.-innerHTML %) (.-children (person-list comp))) '("Foe, John"))))
        (testing "State"
          (reset-state! *crud)
          (u/input-element! (name-field comp) {:target {:value "John"}})
          (u/input-element! (surname-field comp) {:target {:value "Doe"}})
          (are [expected actual] (= expected actual)
                                 "John" (:name-insertion @*crud)
                                 "Doe" (:surname-insertion @*crud))
          (u/click-element! (create-button comp))
          (are [expected actual] (= expected actual)
                                 nil (:name-insertion @*crud)
                                 nil (:surname-insertion @*crud)
                                 {0 {:id 0, :name "John", :surname "Doe"}} (:person/by-id @*crud))
          (u/click-element! (-> (person-list comp) .-children first))
          (are [expected actual] (= expected actual)
                                 "John" (:name-insertion @*crud)
                                 "Doe" (:surname-insertion @*crud))
          (u/input-element! (surname-field comp) {:target {:value "Foe"}})
          (u/click-element! (update-button comp))
          (u/click-element! (-> (person-list comp) .-children first))
          (are [expected actual] (= expected actual)
                                 "John" (:name-insertion @*crud)
                                 "Foe" (:surname-insertion @*crud))
          (u/click-element! (delete-button comp))
          (are [expected actual] (= expected actual)
                                 nil (:name-insertion @*crud)
                                 nil (:surname-insertion @*crud)
                                 {} (:person/by-id @*crud)))))))


