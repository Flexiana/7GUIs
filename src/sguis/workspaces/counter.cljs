(ns sguis.workspaces.counter
  (:require [reagent.core :as r]))

(def counter-state
  (r/atom {:click-count 0}))

(defn counter-ui [counter-state]
  [:<>
   [:div "I have been clicked " (-> counter-state
                                    deref
                                    :click-count) " times."]
   [:button  {:on-click #(swap! counter-state update :click-count inc)}
    "Increase"]
   [:button {:on-click #(swap! counter-state assoc :click-count 0)}
    "Reset"]])
