(ns sguis.workspaces.counter
  (:require [reagent.core :as r]))

(def counter-start
  {:click-count 0})

(def *counter
  (r/atom counter-start))

(defn counter-ui
  [counter-state]
  (let [{:keys [click-count]} @counter-state]
    [:div {:style {:padding "1em"}}
     [:fieldset {:id   "counter"
                 :type "number"} (str click-count)]
     [:button {:id       "increase"
               :on-click #(swap! counter-state update :click-count inc)}
      "Increase"]
     [:button {:id       "reset"
               :on-click #(swap! counter-state assoc :click-count 0)}
      "Reset"]]))
