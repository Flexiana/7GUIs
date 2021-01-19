(ns sguis.workspaces.counter-test
  (:require [sguis.workspaces.counter :refer [counter-ui counter-start]]
            [cljs.test :as t
             :include-macros true
             :refer [is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]))

(ws/deftest counter-tests
  (let [get-counter-elem (fn [comp]
                           (-> comp
                               .-container
                               (.querySelector "#counter")
                               .-innerHTML))
        increase-elem    (fn [comp]
                           (-> comp
                               .-container
                               (.querySelector "#increase")))
        reset-elem       (fn [comp]
                           (-> comp
                               .-container
                               (.querySelector "#reset")))
        *state           (r/atom counter-start)]

    (u/with-mounted-component [counter-ui *state]
      (fn [comp]
        (testing "Initial render."
          (is (= "0" (get-counter-elem comp))))

        (testing "Increase button."
          (u/click-element! (increase-elem comp))
          (u/click-element! (increase-elem comp))
          (is (= "2" (get-counter-elem comp))))

        (testing "Reset button."
          (u/click-element! (reset-elem comp))
          (is (= "0" (get-counter-elem comp))))))))