(ns sguis.workspaces.flight-booker-test
  (:require [sguis.workspaces.flight-booker :refer [booker-ui
                                                    booker-start
                                                    parse-date]]
            [cljs.test :as t
             :include-macros true
             :refer [is testing]]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [sguis.workspaces.test-utils :as u]))

(ws/deftest parse-date-test
  (is (= #inst "2020-02-29T03:00:00.000-00:00" (parse-date "2020.02.29"))))
