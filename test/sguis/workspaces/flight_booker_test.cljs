(ns sguis.workspaces.flight-booker-test
  (:require
    [cljs.test :as t
     :include-macros true
     :refer [is testing]]
    ["date-fns" :as dfns]
    [nubank.workspaces.core :as ws]
    [reagent.core :as r]
    [sguis.workspaces.flight-booker :refer [booker-ui
                                            booker-start
                                            format-date
                                            parse-date
                                            can-book?
                                            format-msg]]
    [sguis.workspaces.test-utils :as u]))

(def testing-dates
  (let [today (dfns/startOfToday)]
    {:today     today
     :yesterday (format-date (dfns/subDays today 1))
     :tomorrow  (format-date (dfns/addDays today 1))
     :future    (format-date (dfns/addDays today 40))}))

(ws/deftest parse-date-specific-format-test
  (let [february-idx 1]
    (is (= (js/Date. 2020 february-idx 29) (parse-date "2020.02.29")))))

(ws/deftest can-book-one-way?-test
  (let [{:keys [today yesterday tomorrow]} testing-dates]
    (letfn [(today-can-book?
              [booker]
              (can-book? booker today))]
      (is (false? (today-can-book? {:flight-type :one-way-flight
                                    :go-flight   yesterday})))
      (is (true? (today-can-book? {:flight-type :one-way-flight
                                   :go-flight   (format-date today)})))
      (is (true? (today-can-book? {:flight-type :one-way-flight
                                   :go-flight   tomorrow}))))))

(ws/deftest one-way-flight-ui-test
  (let [{:keys [today yesterday tomorrow]} testing-dates
        go-flight-input                    #(.getByTestId % "go-flight")
        book-btn                           #(.getByText % "Book")
        can-not-book?                      #(.-disabled (book-btn %))
        reset-btn                          #(.getByTestId % "reset-button")
        *booker                            (r/atom (booker-start today))]
    (u/with-mounted-component
      [booker-ui *booker today]
      (fn [comp]
        (testing "Cannot book for yesterday"
          (u/input-element! (go-flight-input comp) yesterday)
          (is (can-not-book? comp)))

        (testing "Can book today"
          (u/input-element! (go-flight-input comp) (format-date today)))

        (testing "Can book tomorrow"
          (u/input-element! (go-flight-input comp) tomorrow)
          (u/click-element! (book-btn comp))
          (is (= (format-msg @*booker) (.-textContent (.getByTestId comp "book-msg")))))
        (u/click-element! (reset-btn comp))))))

(ws/deftest can-book-return?-test
  (let [{:keys [today yesterday tomorrow future]} testing-dates]
    (letfn [(today-can-book?
              [booker]
              (can-book? booker today))]
      (is (false? (today-can-book? {:flight-type   :return-flight
                                    :go-flight     yesterday
                                    :return-flight tomorrow})))
      (is (true? (today-can-book? {:flight-type   :return-flight
                                   :go-flight     (format-date today)
                                   :return-flight tomorrow})))
      (is (true? (today-can-book? {:flight-type   :return-flight
                                   :go-flight     tomorrow
                                   :return-flight tomorrow})))
      (is (true? (today-can-book? {:flight-type   :return-flight
                                   :go-flight     tomorrow
                                   :return-flight future}))))))

(ws/deftest return-flight-ui-test
  (let [{:keys [today yesterday tomorrow future]} testing-dates
        flight-selector                           #(.getByTestId % "flight-selector")
        go-flight-input                           #(.getByTestId % "go-flight")
        return-flight-input                       #(.getByTestId % "return-flight")
        book-btn                                  #(.getByText % "Book")
        can-not-book?                             #(.-disabled (book-btn %))
        book-msg                                  #(.getByTestId % "book-msg")
        reset-btn                                 #(.getByTestId % "reset-button")
        *booker                                   (r/atom (booker-start today))]
    (u/with-mounted-component
      [booker-ui *booker today]
      (fn [comp]
        (testing "Cannot book yesterday"
          (u/change-element! (flight-selector comp) "return-flight")
          (u/input-element! (go-flight-input comp) yesterday)
          (u/input-element! (return-flight-input comp) tomorrow)
          (is (can-not-book? comp)))

        (testing "Can book today"
          (u/change-element! (flight-selector comp) "return-flight")
          (u/input-element! (go-flight-input comp) (format-date today))
          (u/input-element! (return-flight-input comp) tomorrow)
          (u/click-element! (book-btn comp))
          (is (= (format-msg @*booker) (.-textContent (book-msg comp)))))
        (u/click-element! (reset-btn comp))

        (testing "Can book sameday"
          (u/change-element! (flight-selector comp) "return-flight")
          (u/input-element! (go-flight-input comp) tomorrow)
          (u/input-element! (return-flight-input comp) tomorrow)
          (u/click-element! (book-btn comp))
          (is (= (format-msg @*booker) (.-textContent (book-msg comp))))
          (u/click-element! (reset-btn comp)))

        (testing "Can book in future."
          (u/change-element! (flight-selector comp) "return-flight")
          (u/input-element! (go-flight-input comp) tomorrow)
          (u/input-element! (return-flight-input comp) future)
          (u/click-element! (book-btn comp))
          (is (= (format-msg @*booker) (.-textContent (book-msg comp)))))
        (u/click-element! (reset-btn comp))))))
