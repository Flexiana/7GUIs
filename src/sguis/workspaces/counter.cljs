(ns sguis.workspaces.counter
  (:require [reagent.core :as r]))

(defn counter-ui []
  (r/with-let [*counter-state (r/atom 0)]
    [:div
     [:input {:type "text"
              :data-testid "counter-value"
              :style {:text-align :center}
              :disabled true
              :size 6
              :value @*counter-state}]
     [:button
      {:on-click #(swap! *counter-state inc)}
      "Count"]]))
