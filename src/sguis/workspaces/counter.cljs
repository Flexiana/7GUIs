(ns sguis.workspaces.counter
  "7GUIs counter."
  (:require
    [reagent.core :as r]))

(defn counter-ui
  "Creates a text field with \"0\" and a button to increment that"
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
       {:style    {:margin-top "0.5em"}
        :on-click #(swap! *counter-state inc)}
       "Count"]]]))
