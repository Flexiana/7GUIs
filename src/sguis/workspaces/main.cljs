(ns sguis.workspaces.main
  (:require [reagent.core :as r]
            [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]))

(defonce init (ws/mount))


(defonce click-count (r/atom 0))

(defn state-ful-with-atom []
  [:<>
   [:div {:on-click #(swap! click-count inc)}
    "I have been clicked " @click-count " times."]
   [:div [:button {:on-click #(reset! click-count 0)}
          "button"]]])

(ws/defcard counter-example-card
  (let [counter (atom 0)]
    (ct.react/react-card
     counter
     (r/as-element [state-ful-with-atom]))))
