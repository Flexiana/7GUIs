(ns sguis.workspaces.flight-booker
  (:require ["date-fns" :refer [parseISO
                                isAfter
                                isMatch
                                isSameDay
                                isValid]]
            [clojure.string :as str]
            [reagent.core :as r]
            [cljs.pprint :as pp]))

(def booker-state
  (r/atom {:book-flight :one-way-flight}))

(defn dateformat-coerce [date]
  (when (isMatch date "yyyy.MM.dd")
    (-> date
        (str/replace #"\." "-")
        parseISO)))

(defn can-book? [{:keys [book-flight go-flight return-flight]}]
  (let [today (js/Date.)]
    (cond (= :one-way-flight book-flight) (let [go-flight-parsed (dateformat-coerce go-flight)]
                                            (or (isSameDay go-flight-parsed today)
                                                (isAfter go-flight-parsed today)))
          (= :return-flight book-flight)  (let [go-flight-parsed     (dateformat-coerce go-flight)
                                                return-flight-parsed (dateformat-coerce return-flight)]
                                            (isAfter return-flight-parsed go-flight-parsed)))))

(defn valid-date-style [form-value style]
  (merge style
         (when-not (or (str/blank? form-value)
                       (isMatch form-value "yyyy.MM.dd"))
           {:background-color "red"})))

(defn flight-selector [booker-state]
  [:label {:id "book-flight"}
   [:select {:id        "book-selector"
             :on-change #(swap! booker-state assoc :book-flight (keyword (.. % -target -value)))}
    [:option {:value "one-way-flight"} "one-way flight"]
    [:option {:value "return-flight"} "return flight"]]])

(defn go-flight-input [booker-state]
  (let [{:keys [go-flight]} @booker-state]
    [:div
     [:label {:id "go-flight"}
      [:input {:type      "text"
               :style     (valid-date-style go-flight {})
               :on-change #(swap! booker-state assoc :go-flight  (.. % -target -value))}]]]))

(defn return-flight-input [booker-state]
  (let [{:keys [book-flight
                return-flight]} @booker-state]
    [:div
     [:label {:id "return-flight"}
      [:input {:type      "text"
               :style     (valid-date-style return-flight {})
               :on-change #(swap! booker-state assoc :return-flight  (.. % -target -value))
               :disabled  (false? (= :return-flight book-flight))}]]]))

(defn book-button [booker-state]
  [:button {:disabled (not (can-book? @booker-state))}
   "Book!"])

(defn booker-ui [booker-state]
  [:div {:style {:padding "1em"}}
   [:div "Book a flight ✈️"]
   [flight-selector booker-state]
   [go-flight-input booker-state]
   [return-flight-input booker-state]
   [book-button booker-state]
   #_[:pre (with-out-str (pp/pprint @booker-state))]])
