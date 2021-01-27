(ns sguis.workspaces.counter
  (:require
    [reagent.core :as r]))

(defn counter-ui
  []
  (r/with-let [*counter-state (r/atom 0)]
    [:div.panel.is-primary
     [:div.panel-heading "Counter"]
     [:div.panel-block.is-block
      [:div.control
       [:input.input.has-text-centered
        {:type        :text
         :read-only   true
         :data-testid "counter-value"
         :size        6
         :value       @*counter-state}]]
      [:button.button.is-info
       {:on-click #(swap! *counter-state inc)}
       "Count"]]]))
