(ns sguis.workspaces.flight-booker
  (:require ["date-fns" :refer [parseISO
                                isAfter
                                isMatch
                                isSameDay]]
            [clojure.string :as str]
            [reagent.core :as r]
            [cljs.pprint :as pp]))

(def parse-date-format
  "yyyy.MM.dd")

(def *booker
  (r/atom {:book-flight :one-way-flight}))

(defn parse-date [date]
  (when (isMatch date parse-date-format)
    (-> date
        (str/replace #"\." "-")
        parseISO)))

(defn can-book-one-way-flight? [{:keys [go-flight]} today]
  (let [go-flight-parsed (parse-date go-flight)]
    (or (isSameDay go-flight-parsed today)
        (isAfter go-flight-parsed today))))

(defn can-book-return-flight?
  [booker]
  (apply isAfter
         ((juxt :return-flight :go-flight)
          (-> booker
              (update :go-flight parse-date)
              (update :return-flight parse-date)))))

(defn can-book? [{:keys [book-flight] :as booker} today]
  (case book-flight
    :one-way-flight (can-book-one-way-flight? booker today)
    :return-flight  (can-book-return-flight? booker)))

(defn valid-date-style [form-value style]
  (merge style
         (when-not (or (str/blank? form-value)
                       (isMatch form-value parse-date-format))
           {:background-color "red"})))

(defn flight-selector [booker]
  [:label {:id "book-selector"}
   [:select {:id        "book-selector"
             :on-change #(swap! booker assoc :book-flight (keyword (.. % -target -value)))}
    [:option {:value "one-way-flight"} "one-way flight"]
    [:option {:value "return-flight"} "return flight"]]])

(defn go-flight-input [booker]
  (let [{:keys [go-flight]} @booker]
    [:div
     [:label {:id "go-flight"}
      [:input {:type      "text"
               :style     (valid-date-style go-flight {})
               :on-change #(swap! booker assoc :go-flight  (.. % -target -value))}]]]))

(defn return-flight-input [booker]
  (let [{:keys [book-flight
                return-flight]} @booker]
    [:div
     [:label {:id "return-flight"}
      [:input {:type      "text"
               :style     (valid-date-style return-flight {})
               :on-change #(swap! booker assoc :return-flight  (.. % -target -value))
               :disabled  (false? (= :return-flight book-flight))}]]]))

(defn book-button [booker today]
  [:button {:disabled (not (can-book? @booker today))}
   "Book!"])

(defn booker-ui [booker today]
  [:div {:style {:padding "1em"}}
   [:div "Book a flight ✈️"]
   [flight-selector booker]
   [go-flight-input booker]
   [return-flight-input booker]
   [book-button booker today]
   [:pre (with-out-str (pp/pprint @booker))]])
