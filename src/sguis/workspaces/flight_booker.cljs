(ns sguis.workspaces.flight-booker
  (:require ["date-fns" :refer [parseISO
                                isAfter
                                isMatch
                                isSameDay]]
            [clojure.string :as str]
            [reagent.core :as r]))

(def parse-date-format
  "yyyy.MM.dd")

(def booker-start
  {:book-flight   :one-way-flight
   :go-flight     ""
   :return-flight ""})

(def *booker
  (r/atom booker-start))

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
  [booker today]
  (and (can-book-one-way-flight? booker today)
       (apply isAfter
              ((juxt :return-flight :go-flight)
               (-> booker
                   (update :go-flight parse-date)
                   (update :return-flight parse-date))))))

(defn can-book? [{:keys [book-flight] :as booker} today]
  (case book-flight
    :one-way-flight (can-book-one-way-flight? booker today)
    :return-flight  (can-book-return-flight? booker today)))

(defn valid-date-style [form-value style]
  (merge style
         (when-not (or (str/blank? form-value)
                       (isMatch form-value parse-date-format))
           {:background-color "red"})))

(defn select-booking! [*booker e]
  (swap! *booker assoc :book-flight (keyword (.. e -target -value))))

(defn flight-selector [select-booking!]
  [:label
   [:select {:id        "flight-selector"
             :on-change select-booking!}
    [:option {:value "one-way-flight"} "one-way flight"]
    [:option {:value "return-flight"} "return flight"]]])

(defn go-flight-change! [*booker e]
  (swap! *booker assoc :go-flight (.. e -target -value)))
(defn go-flight-input [{:keys [go-flight]} go-flight-change!]
  [:div
   [:label
    [:input {:id        "go-flight"
             :type      "text"
             :style     (valid-date-style go-flight {})
             :on-change go-flight-change!}]]])

(defn return-flight-change! [*booker e]
  (swap! *booker assoc :return-flight  (.. e -target -value)))

(defn return-flight-input [{:keys [book-flight return-flight]}
                           return-flight-change!]
  [:div
   [:label
    [:input {:id        "return-flight"
             :type      "text"
             :style     (valid-date-style return-flight {})
             :on-change return-flight-change!
             :disabled  (false? (= :return-flight book-flight))}]]])

(defn book-button [booker today]
  (when (can-book? booker today)
    [:button {:id "book-button"}
     "Book!"]))

(defn booker-ui [*booker today]
  [:div {:style {:padding "1em"}}
   [:div "Book a flight ✈️"]
   [flight-selector (partial select-booking! *booker)]
   [go-flight-input @*booker (partial go-flight-change! *booker)]
   [return-flight-input @*booker (partial return-flight-change! *booker)]
   [book-button @*booker today]])
