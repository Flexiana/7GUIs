(ns sguis.workspaces.temperature
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [sguis.workspaces.utils :as u]
    [sguis.workspaces.validator :as valid]))

(def temperature-start
  {:celsius    {:value nil}
   :fahrenheit {:value nil}})

(def other
  {:celsius    :fahrenheit
   :fahrenheit :celsius})

(defmulti convert (fn [from to _] [from to]))

(defmethod convert [:celsius :fahrenheit] [_ _ degrees]
  (-> degrees (* 9.0) (/ 5.0) (+ 32.0) js/Math.round))

(defmethod convert [:fahrenheit :celsius] [_ _ degrees]
  (-> degrees (- 32.0) (* 5.0) (/ 9.0) js/Math.round))

(defn change-temperature
  [state from data]
  (let [to     (other from)
        state' (assoc state from {:value data})]
    (if (valid/float? data)
      (assoc state' to {:value (convert from to data)})
      (-> state'
          (assoc-in [from :invalid?] true)
          (assoc-in [to :unsynced?] true)))))

(defn apply-temperature-change
  [state from data]
  (if (string/blank? data)
    temperature-start
    (change-temperature state from data)))

(defn change-state!
  [state field-key field]
  (let [data (-> field .-target .-value)]
    (swap! state apply-temperature-change field-key data)))

(defn degree-input
  [{:keys [label on-change state]}]
  (let [field-id                           (gensym)
        {:keys [value invalid? unsynced?]} state]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label
       {:for field-id}
       label]]
     [:div.field-body.is-flex-grow-1
      [:div.field
       [:input.input.has-text-centered
        {:id        field-id
         :type      :text
         :class     (u/classes
                      (when invalid? :is-danger)
                      (when unsynced? :is-warning))
         :size      6
         :on-change on-change
         :value     value}]]]]))

(defn temperature-ui
  ([]
   (r/with-let [state (r/atom temperature-start)]
               [temperature-ui state]))
  ([state]
   (let [change-temperature (partial change-state! state)]
     [:div
      [degree-input {:label     "Celsius"
                     :state     (:celsius @state)
                     :on-change (partial change-temperature :celsius)}]
      [degree-input {:label     "Fahrenheit"
                     :state     (:fahrenheit @state)
                     :on-change (partial change-temperature :fahrenheit)}]])))
