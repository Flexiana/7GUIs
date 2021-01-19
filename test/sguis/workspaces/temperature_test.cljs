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

(ws/deftest temperature-tests
  (letfn [(get-celsius-input [comp]
            (-> comp
                .-container
                (.querySelector "#Celsius-input")))
          (get-fahrenheit-input [comp]
            (-> comp
                .-container
                (.querySelector "#Fahrenheit-input")))]
    (u/with-mounted-component [temperature-ui (r/atom temperature-start)]
      (fn [comp]

        (u/change-element! (get-celsius-input comp) {:target {:value "5"}})
        (u/change-element! (get-fahrenheit-input comp)  {:targt {:value (-> (get-fahrenheit-input comp)
                                                                            .-value)}})
        (is (= "5" (-> (get-celsius-input comp)
                       .-value)))))))
