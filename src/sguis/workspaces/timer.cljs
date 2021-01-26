(ns sguis.workspaces.timer
  (:require [reagent.core :as r]))

;; all times are in seconds

(def max-duration 60)

(def timer-start
  {:elapsed-time 0
   :duration     (/ max-duration 2)})

(defn capped-value [{:keys [elapsed-time duration]}]
  (min elapsed-time duration))

(defn progress-bar [{:keys [duration] :as timer-state}]
  [:div.field
   [:progress.progress.is-full-width.is-primary
    {:data-testid "elapsed-seconds-progress"
     :value       (capped-value timer-state)
     :max         duration}]])

(defn countdown-component [timer-state]
  [:div.field
   [:label.label
    {:data-testid "elapsed-seconds"}
    (str (capped-value timer-state) "s")]])

(defn change-duration! [timer-state e]
  (swap! timer-state assoc :duration (.. e -target -valueAsNumber)))

(defn duration-change [timer-state]
  (let [duration (:duration @timer-state)
        field-id (gensym)]
    [:div.field.is-flex.is-flex-direction-column
     [:div.is-align-self-center.has-text-success-dark
      [:output {:for field-id} (str duration "s")]]
     [:input.slider.is-fullwidth.is-success.is-circle.has-output-tooltip
      {:type        :range
       :id          field-id
       :data-testid "duration"
       :min         0
       :max         max-duration
       :value       duration
       :on-input    (partial change-duration! timer-state)}]]))

(defn reset-button-ui [timer-state]
  [:button.button.is-primary
   {:on-click #(swap! timer-state assoc :elapsed-time 0)}
   "Reset!"])

(defn update-elapsed-time! [*timer-state]
  (when (< (:elapsed-time @*timer-state) (:duration @*timer-state))
    (swap! *timer-state update :elapsed-time inc)))

(defn timer-ui
  ([]
   (r/with-let [*timer-state (r/atom timer-start)]
     [timer-ui *timer-state]))
  ([*timer-state]
   (r/with-let [interval (js/setInterval (partial update-elapsed-time! *timer-state) 1000)]
     [:div.panel.is-primary
      {:style {:min-width "24em"}}
      [:div.panel-heading "Timer ⏲️"]
      [:div.panel-block.is-block
       [countdown-component @*timer-state]
       [progress-bar @*timer-state]
       [duration-change *timer-state]
       [reset-button-ui *timer-state]]]
     (finally (js/clearInterval interval)))))
