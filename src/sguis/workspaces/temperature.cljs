(ns sguis.workspaces.temperature
  (:require [reagent.core :as r]
            [sguis.workspaces.validator :as valid]))

(def temperature-start
  {:celsius    nil
   :fahrenheit nil})

(def *temperature
  (r/atom temperature-start))

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

(defn degree-input [{:keys [on-change label value]}]
  [:label {:id label}
   [:input {:data-testid label
            :type        "number"
            :on-change   (partial on-change)
            :value       value}]
   label])

(defn temperature-ui [temperature-state]
  [:div {:style {:padding "1em"}}
   [degree-input {:on-change (partial convert! temperature-state ->fahrenheit)
                  :label     "Celsius"
                  :value     (:celsius @temperature-state)}]
   [degree-input {:on-change (partial convert! temperature-state ->celsius)
                  :label     "Fahrenheit"
                  :value     (:fahrenheit @temperature-state)}]])
