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

(defn numeric? [x]
  (and (number? x) (not (js/Number.isNaN x))))

#_(false? (numeric? (js/parseFloat "a")))
#_(true? (numeric? (js/parseFloat "1")))

(defn add-celsius! [state field-data]
  (let [field-value  (-> field-data .-target .-value)
        parsed-value (js/parseFloat field-value)]
    (when (numeric? parsed-value)
      (swap! state assoc
             :celsius parsed-value
             :converter (c->f parsed-value))
      #_(swap! state assoc :converter 0))))

(defn temperature-ui [temperature-state]
  (let [{:keys [celsius
                fahrenheit
                converter]} @temperature-state]
    [:div {:style {:padding "1em"}}
     [:div [:input {:type      "number"
                    :on-change (partial add-celsius! temperature-state)
                    :value     (if celsius
                                 celsius
                                 converter)}] "Celsius"]
     [:div [:input {:type      "number"
                    :on-change (partial add-celsius! temperature-state)
                    :value     converter}] "Fahrenheit"]
     #_[:button {:on-click #(swap! counter-state update :click-count inc)}
        "Increase"]
     #_[:button {:on-click #(swap! counter-state assoc :click-count 0)}
        "Reset"]
     [:pre (with-out-str (pp/pprint @temperature-state))]]))
