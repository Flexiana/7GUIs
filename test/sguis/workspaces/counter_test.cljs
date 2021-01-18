(ns sguis.workspaces.counter-test
  (:require [sguis.workspaces.counter :refer [counter-ui counter-start]]
            [cljs.test :as t
             :include-macros true
             :refer [is testing use-fixtures]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            ["@testing-library/react" :as rtl]))

(use-fixtures :each
  {:after rtl/cleanup})

(ws/deftest counter-tests
  (let [ui               (-> [counter-ui (r/atom counter-start)]
                             r/as-element
                             rtl/render)
        get-counter-elem (fn []
                           (-> ui
                               .-container
                               (.querySelector "#counter")
                               .-innerHTML))
        increase-elem    (-> ui
                             .-container
                             (.querySelector "#increase"))
        reset-elem       (-> ui
                             .-container
                             (.querySelector "#reset"))]
    (testing "Counter initial state"
      (is (= "0" (get-counter-elem))))

    (testing "Counter Increase"
      (.click rtl/fireEvent increase-elem)
      (r/flush)
      (is (= "1"(get-counter-elem))))

    (testing "Counter Reset"
      (.click rtl/fireEvent reset-elem)
      (r/flush)
      (is (= "0" (get-counter-elem))))))
