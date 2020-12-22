(ns sguis.workspaces.timer
  (:require [reagent.core :as r]
            [cljs.pprint :as pp]))

(def *timer
  (r/atom {}))

(def container-style
  {:position      "relative"
   :width         "130px"
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

(defn progress-bar
  [timer-state]
  (let [{:keys [elapsed-time
                duration]} @timer-state]
    [:div {:style container-style}
     [:div {:style (merge filler-style {:width (when (< elapsed-time duration)
                                                 (-> elapsed-time
                                                     (/  duration)
                                                     (* 100)
                                                     (str "%")))})}]]))

(defn countdown-component [timer-state]
  (let [{:keys [elapsed-time
                duration]} @timer-state]
    (when (< elapsed-time duration)
      (r/with-let [timer-fn (js/setInterval #(swap! timer-state update :elapsed-time inc) 1000)]
        [:div.timer
         [:div (str (:elapsed-time @timer-state) "s")]]
        (finally (js/clearInterval timer-fn))))))

(defn duration-change [timer-state]
  (let [{:keys [elapsed-time]} @timer-state]
    [:div {:class "timer-slider"}
     [:input {:type         "range"
              :min          "1"
              :max          "100"
              :width        "100%"
              :defaultValue "1"
              :on-input     #(let [duration (-> %
                                                .-target
                                                .-valueAsNumber)]
                           (swap! timer-state assoc :duration duration)
                           (swap! timer-state :remaining-time update (- %
                                                                        elapsed-time)))}]]))

(defn timer-ui [timer-state]
  (r/create-class
   {:component-did-mount (swap! timer-state assoc
                                :elapsed-time 0
                                :duration 1)
    :reagent-render (fn []
                      [:div {:style {:padding "1em"}}
                       [:div {:style {:padding "0.5em"}}
                        "Timer ⏲️"]
                       [countdown-component timer-state]
                       [progress-bar timer-state]
                       [duration-change timer-state]
                       [:button {:class "reset-timer"
                                 :on-click #(swap! timer-state assoc :elapsed-time 0)}
                        "Reset!"]
                       [:pre (with-out-str (pp/pprint @timer-state))]])}))
