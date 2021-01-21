(ns sguis.workspaces.timer
  (:require [reagent.core :as r]))

(def *timer
  (r/atom {}))

(def container-style
  {:position      "relative"
   :width         "100%"
   :height        "0.5em"
   :border-radius "50px"
   :padding       0
   :border        "1px solid #333"})

(def filler-style
  {:position "relative"
   :border-radius "50px"
   :box-shadow "0 0 10px 0"
   :height "0.5em"
   :opacity 1
   :color "rgb(178, 34, 34)"
   :background-color "rgb(178, 34, 34)"})

(defn fill-bar [elapsed-time duration]
  (when (< elapsed-time duration)
    (-> elapsed-time
        (/  duration)
        (* 100)
        (str "%"))))

(defn progress-bar
  [timer-state]
  (let [{:keys [elapsed-time
                duration]} @timer-state]
    [:div {:style container-style}
     [:div {:data-testid "progress"
            :style (merge filler-style
                          {:width (fill-bar elapsed-time duration)})}]]))

(defn countdown-component [timer-state]
  (let [{:keys [elapsed-time
                duration]} @timer-state
        finished? (< elapsed-time duration)]
    (if finished?
      (r/with-let [timer-fn (js/setInterval #(swap! timer-state update :elapsed-time inc) 1000)]
        [:div.timer
         [:div
          {:data-testid "timer"}
          (str (:elapsed-time @timer-state) "s")]]
        (finally (js/clearInterval timer-fn)))
      [:div.timer
       [:div (str (:elapsed-time @timer-state) "s")]])))

(defn duration-change [timer-state]
  [:input {:style        {:width "100%"}
           :type         "range"
           :data-testid "range"
           :min          "1"
           :max          "100"
           :defaultValue "1"
           :on-input     #(swap! timer-state
                                 assoc
                                 :duration
                                 (.. %
                                     -target
                                     -valueAsNumber))}])

(defn reset-button-ui [timer-state]
  [:button {:class "reset-timer"
            :on-click #(swap! timer-state assoc :elapsed-time 0)}
   "Reset!"])

(defn timer-ui [timer-state]
  (r/create-class
   {:component-did-mount (swap! timer-state
                                assoc
                                :elapsed-time 0
                                :duration 1)
    :reagent-render (fn []
                      [:div {:style {:padding "1em"
                                     :width "10vw"}}
                       [:div {:style {:padding "0.5em"}}
                        "Timer ⏲️"]
                       [countdown-component timer-state]
                       [progress-bar timer-state]
                       [duration-change timer-state]
                       [reset-button-ui timer-state]])}))
