(ns sguis.workspaces.timer-test
  (:require [sguis.workspaces.timer :refer [timer-ui
                                            *timer]]
            [cljs.test :as t
             :include-macros true
             :refer [is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]))
(ws/deftest
  timer-test
  (let []
    (testing "Initialization"
      true)))

