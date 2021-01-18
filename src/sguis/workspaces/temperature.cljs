(ns sguis.workspaces.temperature
  (:require [reagent.core :as r]
            [sguis.workspaces.validator :as valid]
            [clojure.string :as str]))

(def *temperature
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


(defn to-celsius [current-state data]
  (assoc current-state
         :celsius data
         :fahrenheit (celsius->fahrenheit data)))

(defn to-fahrenheit [current-state data]
  (assoc current-state
         :celsius (fahrenheit->celsius data)
         :fahrenheit data))

(defn convert! [temperature-state to field]
  (let [target (-> field .-target)
        data (.-valueAsNumber target)]
    (if (str/blank? (.-value target))
      (swap! temperature-state assoc
             :fahrenheit ""
             :celsius "")
      (when (valid/numeric? data) (swap! temperature-state to data)))))

(defn degree-input [temperature-state {:keys [on-change label unit]}]
  [:label [:input {:type      "number"
                   :on-change (partial on-change temperature-state)
                   :value     (get @temperature-state unit)}]
   label])

(defn temperature-ui [temperature-state]
    [:div {:style {:padding "1em"}}
     (degree-input temperature-state {:on-change (partial convert! temperature-state to-fahrenheit) 
                                      :label "Celsius" 
                                      :unit :celsius})
     (degree-input temperature-state {:on-change (partial convert! temperature-state to-celsius)
                                      :label "Fahrenheit"
                                      :unit :fahrenheit})])
