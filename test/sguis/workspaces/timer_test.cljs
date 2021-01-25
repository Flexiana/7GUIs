(ns sguis.workspaces.timer-test
  (:require [sguis.workspaces.timer :refer [timer-ui
                                            timer-start]]
            [cljs.test :as t
             :include-macros true
             :refer [are is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]))

(ws/deftest timer-test
  (let [fake-timer   (u/install-timer)
        *test-timer  (r/atom timer-start)
        reset-button #(.getByText % "Reset!")
        label        #(.getByTestId % "elapsed-seconds")
        input        #(.getByTestId % "duration")
        progress     #(.getByTestId % "elapsed-seconds-progress")]
    (u/with-mounted-component
      [timer-ui *test-timer]
      (fn [comp]
        (testing "Initialization"
          (are [expected actual] (= expected actual)
            "0s" (.-innerHTML (label comp))
            "30" (.-value (input comp))
            "0"  (.-min (input comp))
            "60" (.-max (input comp))
            0    (.-value (progress comp))))

        (testing "Run"
          (u/input-element! (input comp) "10")
          (is (= 0 (.-value (progress comp))))
          (u/tick fake-timer 5000)
          (is (= {:elapsed-time 5, :duration 10} @*test-timer))
          (r/flush)
          (is (= "5s" (.-innerHTML (label comp))))
          (is (= 5 (.-value (progress comp)))))

        (testing "Reset button"
          (let [duration (:duration @*test-timer)]
            (u/click-element! (reset-button comp))
            (is (= {:elapsed-time 0, :duration duration}  @*test-timer))
            (u/tick fake-timer 10000)
            (is (= {:elapsed-time 10 :duration duration} @*test-timer))
            (r/flush)
            (is (= 10 (.-value (progress comp))))))
        (testing "Should not overrun"
          (u/click-element! (reset-button comp))
          (u/input-element! (input comp) "5")
          (is (= {:elapsed-time 0, :duration 5}  @*test-timer))
          (u/tick fake-timer 10000)
          (is (= {:elapsed-time 5 :duration 5} @*test-timer))
          (r/flush)
          (is (= 5 (.-value (progress comp))))
          (u/input-element! (input comp) "15")
          (is (= {:elapsed-time 5 :duration 15} @*test-timer))
          (r/flush)
          (is (= 5 (.-value (progress comp)))))

        (u/uninstall-timer fake-timer)))))
