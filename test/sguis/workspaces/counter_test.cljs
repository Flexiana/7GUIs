(ns sguis.workspaces.counter-test
  (:require [sguis.workspaces.counter :refer [counter-ui *counter]]
            [cljs.test :as t
             :include-macros true
             :refer [is testing use-fixtures]]
            [nubank.workspaces.core :as ws]
            [dommy.core :as dommy :refer-macros [sel1]]
            [reagent.core :as r]
            ["@testing-library/react" :as rtl]))

(use-fixtures :each
  {:after rtl/cleanup})

(ws/deftest counter-tests
  (let [ui (->> [counter-ui *counter]
                r/as-element
                rtl/render)]
    (testing "Counter initial state"
      (is (= "0"
             (->  [:#counter]
                  sel1
                  .-innerHTML))))))
