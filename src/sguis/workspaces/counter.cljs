(ns sguis.workspaces.counter
  (:require [reagent.core :as r]))

(def counter-state
  (r/atom {:click-count 0}))

(defn counter-ui
  [counter-state]
  (let [{:keys [click-count]} @counter-state]
    [:div {:style {:padding "1em"}}
     [:fieldset {:type "number"} (str click-count)]
     [:button {:on-click #(swap! counter-state update :click-count inc)}
      "Increase"]
     [:button {:on-click #(swap! counter-state assoc :click-count 0)}
      "Reset"]]))
