(ns sguis.workspaces.temperature
  (:require [reagent.core :as r]
            [cljs.pprint :as pp]))

(def temperature-state
  (r/atom {}))

(defn c->f [c]
  (-> c
      (* 9.0)
      (/ 5.0)
      (+ 32.0)))

(defn f->c [f]
  (-> f
      (- 32.0)
      (* 5.0)
      (/ 9.0)))

#_(== 5 (f->c (c->f 5)))

(defn add-celsius [state data]
  (js/console.log (-> data .-target .-value js/parseFloat))
  #_(swap! state assoc :celsius ))

(defn temperature-ui [temperature-state]
  (let [{:keys [celsius
                fahrenheit]} @temperature-state]
    [:div {:style {:padding "1em"}}
     [:div [:input {:on-change (partial add-celsius temperature-state)}] "Celsius"]
     #_[:button {:on-click #(swap! counter-state update :click-count inc)}
        "Increase"]
     #_[:button {:on-click #(swap! counter-state assoc :click-count 0)}
        "Reset"]
     [:pre (with-out-str (pp/pprint @temperature-state))]]))
