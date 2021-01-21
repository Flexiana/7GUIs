(ns sguis.workspaces.timer-test
  (:require [sguis.workspaces.timer :refer [timer-ui timer-start]]
            [cljs.test :as t
             :include-macros true
             :refer [is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]))


(ws/deftest timer-ui-test
  (let [*timer          (r/atom timer-start)
        timer-component #(.getByTestId % "timer-component")]
    (u/with-mounted-component [timer-ui *timer]
      (fn [comp]
        (js/console.log (timer-component comp))))))
