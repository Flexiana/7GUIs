(ns sguis.workspaces.temperature-test
  (:require [sguis.workspaces.temperature :refer [temperature-ui
                                                  temperature-start
                                                  celsius->fahrenheit
                                                  fahrenheit->celsius]]
            [cljs.test :as t
             :include-macros true
             :refer [is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]))

(ws/deftest fahrenheit<->celsius-test
  (is (== 5 (fahrenheit->celsius (celsius->fahrenheit 5)))))

(ws/deftest temperature-ui-tests
  (let [celsius-input    #(.getByTestId % "Celsius")
        fahrenheit-input #(.getByTestId % "Fahrenheit")]
    (u/with-mounted-component [temperature-ui (r/atom temperature-start)]
      (fn [comp]
        (let [_celsius->fahrenheit!     (u/change-element! (celsius-input comp) {:target {:value "5"}})
              celsius->fahrenheit-value (.-value (fahrenheit-input comp))
              _fahrenheit->celsius!     (u/change-element! (fahrenheit-input comp) {:target {:value celsius->fahrenheit-value}})
              fahrenheit->celsius-value (.-value (celsius-input comp))]
          (is (= "5" fahrenheit->celsius-value)))))))
