(ns sguis.workspaces.main
  (:require [reagent.core :as r]
            [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]))

(defonce init (ws/mount))

(ws/defcard counter-example-card
  (let [click-count (r/atom 0)]
    (ct.react/react-card
     click-count
     (r/as-element
      [:<>
       [:div "I have been clicked " @click-count " times."]
       [:button  {:on-click #(swap! click-count inc)}
        "Increase"]
       [:button {:on-click #(reset! click-count 0)}
        "Reset"]]))))
