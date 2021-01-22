(ns sguis.workspaces.timer-test
  (:require [sguis.workspaces.timer :refer [timer-ui
                                            *timer]]
            [cljs.test :as t
             :include-macros true
             :refer [are is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]))

(defn wait [milliseconds f]
  (js/setTimeout f milliseconds))

(ws/deftest
  timer-test
  (let [fake-timer (u/install-timer)
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
                                 "0%" (.getPropertyValue (.-style (progress comp)) "width")))
        (testing "Run"
          (u/input-element! (input comp) {:target {:value "10"}})
          (u/tick fake-timer 5000)
          (is (= {:elapsed-time 5, :duration 10} @*test-timer))
          (r/flush)
          (is (= "5s" (.-innerHTML (label comp))))
          (is (= "50%" (.getPropertyValue (.-style (progress comp)) "width")))
          (u/uninstall-timer fake-timer))))))