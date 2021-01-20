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
   :return-flight ""
   :booker-msg    nil})

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
   [:select {:data-testid "flight-selector"
             :on-change   select-booking!}
    [:option {:value "one-way-flight"} "one-way flight"]
    [:option {:value "return-flight"} "return flight"]]])

(defn go-flight-change! [*booker e]
  (swap! *booker assoc :go-flight (.. e -target -value)))
(defn go-flight-input [{:keys [go-flight]} go-flight-change!]
  [:div
   [:label
    [:input {:data-testid "go-flight"
             :type        "text"
             :style       (valid-date-style go-flight {})
             :on-change   go-flight-change!}]]])

(defn return-flight-change! [*booker e]
  (swap! *booker assoc :return-flight  (.. e -target -value)))

(defn return-flight-input [{:keys [book-flight return-flight]}
                           return-flight-change!]
  [:div
   [:label
    [:input {:data-testid "return-flight"
             :type        "text"
             :style       (valid-date-style return-flight {})
             :on-change   return-flight-change!
             :disabled    (false? (= :return-flight book-flight))}]]])

(defn booking-message! [*booker msg _]
  (swap! *booker assoc :booker-msg msg))

(defn format-msg [{:keys [book-flight go-flight return-flight]}]
  (case book-flight
    :one-way-flight (str "✅ You have booked a one-way flight on " go-flight)
    :return-flight  (str "✅ You have booked a return flight for: " go-flight
                         " to: " return-flight)))
(defn book-button [booker today booking-message!]
  [:button {:data-testid "book-button"
            :disabled    (not (can-book? booker today))
            :on-click    (partial booking-message! (format-msg booker))}
   "Book!"])

(defn book-message [{:keys [booker-msg]}]
  (when booker-msg
    [:div {:data-testid "book-msg"}
     booker-msg]))

(defn reset-booker! [*booker _]
  (swap! *booker assoc
         :go-flight     ""
         :return-flight ""
         :booker-msg nil))

(defn reset-button [{:keys [booker-msg]} reset-booker!]
  (when booker-msg
    [:button {:on-click (partial reset-booker!)} "Reset!"]))

(defn booker-ui [*booker today]
  [:div {:style {:padding "1em"}}
   [:div "Book a flight ✈️"]
   [flight-selector (partial select-booking! *booker)]
   [go-flight-input @*booker (partial go-flight-change! *booker)]
   [return-flight-input @*booker (partial return-flight-change! *booker)]
   [book-button @*booker today (partial booking-message! *booker)]
   [book-message @*booker]
   [reset-button @*booker (partial reset-booker! *booker)]])
