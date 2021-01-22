(ns sguis.workspaces.timer-test
  (:require [sguis.workspaces.timer :refer [timer-ui
                                            *timer]]
            [cljs.test :as t
             :include-macros true
             :refer [are is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]))

(ws/deftest
  timer-test
  (let [js-timer (u/install-timer)
        *test-timer (r/atom {})
        reset-button #(.getByText % "Reset!")
        label #(.getByTestId % "timer")
        input #(.getByTestId % "range")
        progress #(.getByTestId % "progress")]
    (u/with-mounted-component
      [timer-ui *test-timer]
      (fn [comp]
        (testing "Initialization"
          (are [expected actual] (= expected actual)
                                 js/HTMLButtonElement (type (reset-button comp))
                                 js/HTMLDivElement (type (label comp))
                                 "0s" (.-innerHTML (label comp))
                                 "1" (.-value (input comp))
                                 "1" (.-min (input comp))
                                 "100" (.-max (input comp))
                                 nil (.-width (progress comp))))
        (testing "Run"
          (u/input-element! (input comp) {:target {:value "10"}})
          (u/tick js-timer 5000)
          (is (= {:elapsed-time 5, :duration 10} @*test-timer))
          (u/uninstall-timer js-timer))))))
