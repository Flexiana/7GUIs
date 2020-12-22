(ns sguis.workspaces.timer
  (:require [reagent.core :as r]
            [cljs.pprint :as pp]))

(def *timer
  (r/atom {:elapsed-time 50}))

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
  (let [{:keys [elapsed-time]} @timer-state]
    [:div {:style container-style}
     [:div {:style (merge filler-style {:width (-> elapsed-time
                                                   (* -1.0)
                                                   (+ 100.0)
                                                   (str "%"))})}]]))

(defn countdown-component [timer-state]
  (let [timer-fn (js/setInterval #(when-not (<= (@timer-state :elapsed-time) 0)
                                    (swap! timer-state update :elapsed-time dec)) 1000)]
    (r/create-class
     {:display-name "countdown-component"
      :component-did-mount (fn [_]
                             timer-fn)
      :component-will-unmount (fn [_]
                                (js/clearInterval timer-fn))
      :reagent-render (fn []
                        (if-not (<= (@timer-state :elapsed-time) 0)
                          [:div.timer
                           [:div (-> @timer-state :elapsed-time (str "s"))]]
                          [:div ]))})))

(defn duration-change [timer-state]
  [:div {:class "timer-slider"}
   [:input {:type "range"
            :min "1"
            :max "100"
            :step "0.1"
            :on-input (fn [this]
                        (swap! timer-state update :elapsed-time (fn [elapsed-time]
                                                                  (+ elapsed-time (-> this
                                                                                      .-target
                                                                                      .-valueAsNumber
                                                                                      (- 50))))))}]])

(defn timer-ui [timer-state]
  [:div {:style {:padding "1em"}}
   [:div {:style {:padding "0.5em"}}
    "Timer ⏲️"]
   [countdown-component timer-state]
   [progress-bar timer-state]
   [duration-change timer-state]
   [:button {:class "reset-timer"
             :on-click #(swap! timer-state assoc :elapsed-time 50)}
    "Reset!"]
   [:pre (with-out-str (pp/pprint @timer-state))]])
