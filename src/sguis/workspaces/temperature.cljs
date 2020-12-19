(ns sguis.workspaces.temperature
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [cljs.pprint :as pp]))

(def temperature-state
  (r/atom {}))

(defn celsius->fahrenheit [c]
  (-> c
      (* 9.0)
      (/ 5.0)
      (+ 32.0)))

(defn fahrenheit->celsius [f]
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
  (when (numeric? data)
    (assoc current-state
           :celsius data
           :fahrenheit (celsius->fahrenheit data))))

(defn add-celsius! [temperature-state field]
  (let [target (-> field .-target)]
    (if (str/blank? (.-value target))
      (swap! temperature-state assoc
             :celsius ""
             :fahrenheit "")
      (swap! temperature-state add-celsius (.-valueAsNumber target)))))

(defn add-fahrenheit [current-state data]
  (when (numeric? data)
    (assoc current-state
           :celsius (fahrenheit->celsius data)
           :fahrenheit data)))

(defn add-fahrenheit! [temperature-state field]
  (let [target (-> field .-target)]
    (if (str/blank? (.-value target))
      (swap! temperature-state assoc
             :fahrenheit ""
             :celsius "")
      (swap! temperature-state add-fahrenheit (.-valueAsNumber target)))))

(defn temperature-ui [temperature-state]
  (let [{:keys [celsius
                fahrenheit]} @temperature-state]
    [:div {:style {:padding "1em"}}
     [:label [:input {:type          "number"
                      :on-change     (partial add-celsius! temperature-state)
                      :value (str celsius)}]
      "Celsius"]
     [:label [:input {:type          "number"
                      :on-change     (partial add-fahrenheit! temperature-state)
                      :value (str fahrenheit)}]
      "Fahrenheit"]
     #_[:pre (with-out-str (pp/pprint @temperature-state))]]))
