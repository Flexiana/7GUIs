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
  (let [reset-button #(.getByText % "Reset!")
        label #(.getByTestId % "timer")
        input #(.getByTestId % "range")
        progress #(.getByTestId % "progress")]
    (u/with-mounted-component
      [#(timer-ui *timer)]
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
          (reset! *timer {})
          (u/change-element! (input comp) {:target {:value 100}})
          (is (= "100" (.-value (input comp))))
          ;(wait 10000 #(js/alert "most"))
          (is (= "100" (.getPropertyValue (.-style (progress comp)) "width"))))))))
          ;(is (some? (wait 10 (.-value (progress comp)))))
          ;(is (= 100 (wait 100 (.-valueAsNumber (input comp))))))))))
