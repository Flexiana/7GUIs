(ns sguis.workspaces.temperature
  "7GUIs temperature converter"
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [sguis.workspaces.utils :as u]
    [sguis.workspaces.validator :as valid]))

(def temperature-start
  "init values"
  {:celsius    {:value nil}
   :fahrenheit {:value nil}})

(def other
  "Conversion matrix"
  {:celsius    :fahrenheit
   :fahrenheit :celsius})

(defmulti convert (fn [from to _] [from to]))

(defmethod convert [:celsius :fahrenheit] [_ _ degrees]
  ;;convert celsius to fahrenheit
  (-> degrees (* 9.0) (/ 5.0) (+ 32.0) js/Math.round))

(defmethod convert [:fahrenheit :celsius] [_ _ degrees]
  ;;convert fahrenheit to celsius
  (-> degrees (- 32.0) (* 5.0) (/ 9.0) js/Math.round))

(defn apply-temperature-change
  "Apply changes based on user input"
  [state from data]
  (let [to     (other from)
        state' (assoc state from {:value data})]
    (cond (string/blank? data) (reset! state temperature-start)
          (valid/float? data) (assoc state' to {:value (convert from to data)})
          :else (-> state'
                    (assoc-in [from :invalid?] true)
                    (assoc-in [to :unsynced?] true)))))

(defn change-state!
  "Change value action for input fields"
  [state field-key field]
  (let [data (-> field .-target .-value)]
    (swap! state apply-temperature-change field-key data)))

(defn degree-input
  "Input fields"
  [{:keys [label on-change state]}]
  (let [field-id (gensym)
        {:keys [value invalid? unsynced?]} state]
    [:div.field.is-horizontal {:style {:margin-top "0.5em"}}
     [:div.field-label.is-normal
      [:label.label
       {:for field-id}
       label]]
     [:div.field-body.is-flex-grow-2
      [:div.field {:style {:margin-bottom "0.5em"}}
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
  "Temperature converter main UI"
  ([]
   (r/with-let [state (r/atom temperature-start)]
     [temperature-ui state]))
  ([state]
   (let [change-temperature (partial change-state! state)]
     [:div.panel.is-primary
      [:div.panel-heading "Temperature converter"]
      [degree-input {:label     "Celsius"
                     :state     (:celsius @state)
                     :on-change (partial change-temperature :celsius)}]
      [degree-input {:label     "Fahrenheit"
                     :state     (:fahrenheit @state)
                     :on-change (partial change-temperature :fahrenheit)}]])))
