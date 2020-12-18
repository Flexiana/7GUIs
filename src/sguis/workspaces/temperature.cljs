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
             :fahrenheit (c->f parsed-value)))))

(defn add-fahrenheit! [state field-data]
  (let [field-value  (-> field-data .-target .-value)
        parsed-value (js/parseFloat field-value)]
    (when (numeric? parsed-value)
      (swap! state assoc
             :celsius (f->c parsed-value)
             :fahrenheit parsed-value))))

(defn temperature-ui [temperature-state]
  (let [{:keys [celsius
                fahrenheit]} @temperature-state]
    [:div {:style {:padding "1em"}}
     [:div [:input {:type      "number"
                    :on-change (partial add-celsius! temperature-state)
                    :value     (when celsius
                                 celsius)}] "Celsius"]
     [:div [:input {:type      "number"
                    :on-change (partial add-fahrenheit! temperature-state)
                    :value     (when fahrenheit
                                 fahrenheit)}] "Fahrenheit"]
     [:button {:on-click #(reset! temperature-state {})}
      "Reset"]
     #_[:input {:type      "number"
                :on-change #(-> % .-target .-value str/blank? js/console.log)}]
     [:pre (with-out-str (pp/pprint @temperature-state))]]))
