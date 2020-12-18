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

(defn add-celsius [current-state data]
  (if (numeric? data)
    (assoc current-state
           :celsius data
           :err nil
           :fahrenheit (c->f data))
    (assoc current-state
           :err "Invalid Celsius")))

(defn add-celsius! [temperature-state field]
  (swap! temperature-state add-celsius (-> field .-target .-valueAsNumber)))

(defn add-fahrenheit [current-state data]
  (if (numeric? data)
    (assoc current-state
           :celsius (f->c data)
           :err nil
           :fahrenheit data)
    (assoc current-state
           :err "Invalid Fahrenheit")))

(defn add-fahrenheit! [temperature-state field]
  (swap! temperature-state add-fahrenheit (-> field .-target .-valueAsNumber)))

(defn temperature-ui [temperature-state]
  (let [{:keys [celsius
                fahrenheit
                err]} @temperature-state]
    [:div {:style {:padding "1em"}}
     [:label [:input {:type          "number"
                      :on-change     (partial add-celsius! temperature-state)
                      :valueAsNumber celsius
                      :value         (str celsius)}]
      "Celsius"]
     [:label [:input {:type          "number"
                      :on-change     (partial add-fahrenheit! temperature-state)
                      :valueAsNumber fahrenheit
                      :value         (str fahrenheit)}]
      "Fahrenheit"]
     [:button {:on-click #(reset! temperature-state {})}
      "Reset"]
     [:label [:input {:type     "text"
                      :disabled true
                      :value    (str err)}]]
     [:pre (with-out-str (pp/pprint @temperature-state))]]))
