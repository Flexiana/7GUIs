(ns sguis.workspaces.timer
  (:require [reagent.core :as r]
            [clojure.core.async :refer [<! go timeout]]
            [cljs.pprint :as pp]))


(def *timer
  (r/atom {:timer 50}))

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

(defn progress-bar
  [timer-state]
  (let [{:keys [timer]} @timer-state]
    [:div {:style container-style}
     [:div {:style (merge filler-style {:width (-> timer
                                                   (str "%"))})}]]))

(defn countdown-component [timer-state]
  (when-not (zero? (get @timer-state :timer))
    (r/with-let [seconds-left (get @timer-state :timer)
                 timer-fn     (js/setInterval #(swap! timer-state update :timer dec) 1000)]
      [:div.timer
       [:div (str (:timer @timer-state) "s")]]
      (finally (js/clearInterval timer-fn)))))

(defn timer-ui [timer-state]
  [:div {:style {:padding "1em"}}
   [:div {:style {:padding "0.5em"}}
    "Timer ⏲️"]
   [countdown-component timer-state]
   [progress-bar timer-state]
   [:div {:class "timer-slider"}
    [:input {:type "range"
             :min "1"
             :max "100"
             :on-input #(swap! timer-state assoc :timer (-> %
                                                            .-target
                                                            .-valueAsNumber))}]]
   [:button {:class "reset-timer"
             :on-click #(swap! timer-state assoc :timer 50)}
    "Reset!"]])
