(ns sguis.workspaces.counter-test
  (:require
    [cljs.test :as t
     :include-macros true
     :refer [is testing]]
    [nubank.workspaces.core :as ws]
    [sguis.workspaces.counter :refer [counter-ui]]
    [sguis.workspaces.test-utils :as u]))

(ws/deftest counter-tests
  (let [counter-value #(-> % (.getByTestId "counter-value") .-value)
        count-btn #(.getByText % #"(?i)count")]
    (u/with-mounted-component [counter-ui]
                              (fn [comp]
                                (testing "Initial render."
                                  (is (= "0" (counter-value comp))))

                                (testing "Count button."
                                  (u/click-element! (count-btn comp))
                                  (u/click-element! (count-btn comp))
                                  (is (= "2" (counter-value comp))))))))
