(ns sguis.workspaces.counter
  (:require [reagent.core :as r]))

(defn counter-ui []
  (r/with-let [*counter-state (r/atom 0)]
    [:div.field.is-grouped.is-flex.is-justify-content-evenly
     [:div.control
      [:input.input.has-text-centered
       {:type "text"
        :data-testid "counter-value"
        :size 6
        :value @*counter-state}]]
     [:button.button.is-info
      {:on-click #(swap! *counter-state inc)}
      "Count"]]))
