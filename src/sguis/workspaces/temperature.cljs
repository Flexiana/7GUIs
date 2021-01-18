(ns sguis.workspaces.temperature
  (:require [reagent.core :as r]
            [sguis.workspaces.validator :as valid]))

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


(defn ->fahrenheit [current-state data]
  (assoc current-state
           :celsius data
           :fahrenheit (celsius->fahrenheit data)))

(defn ->celsius [current-state data]
  (assoc current-state
         :celsius (fahrenheit->celsius data)
         :fahrenheit data))

(defn convert! [temperature-state to field]
  (let [data (-> field .-target .-valueAsNumber)]
    (if (valid/numeric? data)
      (swap! temperature-state to data)
      (swap! temperature-state assoc
             :fahrenheit ""
             :celsius ""))))

(defn degree-input [temperature-state {:keys [on-change label unit]}]
  [:label [:input {:type      "number"
                   :on-change (partial on-change)
                   :value     (get @temperature-state unit)}]
   label])

(defn temperature-ui [temperature-state]
    [:div {:style {:padding "1em"}}
     (degree-input temperature-state {:on-change (partial convert! temperature-state ->fahrenheit)
                                      :label "Celsius" 
                                      :unit :celsius})
     (degree-input temperature-state {:on-change (partial convert! temperature-state ->celsius)
                                      :label "Fahrenheit"
                                      :unit :fahrenheit})])
