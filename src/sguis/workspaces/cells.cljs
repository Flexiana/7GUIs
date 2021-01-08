(ns sguis.workspaces.cells
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sci.core :refer [eval-string]]))

(def *cells
  (r/atom {:focused-cell nil
           :edition      ""
           :cells        {}}))

(def az-range
  (map char (range 65 91)))

(def table-lines
  (range 0 100))

(def possible-cells
  (set (for [s az-range
             n table-lines]
         (keyword (str s n)))))

;; Parser Impl
(defn numeric? [x]
  (and (number? x) (not (js/Number.isNaN x))))

(def kw->op
  {:add      `+
   :subtract `-
   :div      `/
   :mul      `*
   :mod      `mod
   :sum      `+
   :prod     `*})

(defn can-parse-numeric? [parsed-exp]
  (and (re-find  #"^[0-9]" parsed-exp)
       (not (re-find #"[aA-zZ]" parsed-exp))
       (numeric? (js/parseFloat parsed-exp))))

(defn is-cell? [parsed-exp]
  (and (= 2 (count parsed-exp))
       (possible-cells (keyword (str/upper-case parsed-exp)))))

(defn is-range-cells? [parsed-exp]
  (and (= 5 (count parsed-exp))
       (->> (str/split parsed-exp #":")
            (map (comp boolean possible-cells keyword str/upper-case))
            (every? true?))))

(defn range-cells-get [kw-range]
  (let [[fst snd]     kw-range
        [collmin min] (name fst)
        [collmax max] (name snd)]
    (for [collv (range (.charCodeAt collmin) (inc (.charCodeAt collmax)))
          v     (range (int min) (inc (int max)))]
      (keyword (str (char collv) v)))))

(defn is-op? [parsed-exp]
  ((set (keys kw->op)) (keyword parsed-exp)))

(defn tokenizer [{:keys [cells]} parsed-exp]
  (cond
    (can-parse-numeric? parsed-exp)  (js/parseFloat parsed-exp)
    (is-cell? parsed-exp)           (->> parsed-exp
                                         str/upper-case
                                         keyword
                                         (#(get cells % 0))
                                         js/parseFloat)
    (is-range-cells? parsed-exp)    (->> (-> parsed-exp
                                             (str/upper-case)
                                             (str/split #":")
                                             range-cells-get)
                                         (map keyword)
                                         (map #(get cells % 0))
                                         (map js/parseFloat))
    (is-op? parsed-exp)             (get kw->op (keyword parsed-exp))))

(defn parse [env s]
  (some->> (str/split s #" ")
           (map (partial tokenizer env))
           (remove nil?)
           flatten
           str))

(defn eval-cell [env s]
  (let [low-cased (some-> s str/lower-case)]
    (cond (nil? s)                       ""
          (str/ends-with? low-cased "=") (some-> (parse env low-cased)
                                                 (eval-string {:allow (vals kw->op)})
                                                 str)
          :else                          s)))

;; Manual tests
#_(is (= "10" (eval-cell {:cells {:A2 "2" :B8 "8"}} "Sum of A2:B8 =")))
#_(is (= "0" (eval-cell {} "Sum of A2:B8 ="))
      (= "4" (eval-cell {} "Sum of 4 and A2:B8 ="))
      (= "3" (eval-cell {} "Add 1 and 2 ="))
      (= "NaN" (eval-cell {} "Div of B5 and C5 ="))
      (= "a" (eval-cell {} "a"))
      (= "20" (eval-cell {:cells {:A7 "10"
                                  :G0 "10"}} "Add A7 and G0 ="))
      (= "" (eval-cell {} nil))

      #_(is (= "lol" (tokenizer {:cells {:A3 "lol"}} "A3"))
            (= [:A3 :B5] (tokenizer {} "A3:B5"))
            (= `+ (tokenizer {} "sum"))))

;; UI impl

(def table-style
  {:border          "1px solid black"
   :border-collapse "collapse"
   :width           "100%"
   :overflow        "auto"})

(def overflow-style
  {:overflow "auto"})

(def light-border-style
  {:border  "1px solid #ccc"
   :padding "0.5em"})

(defn header-fn [chars]
  ^{:key chars}
  [:td {:style light-border-style} chars])

(defn focus-cell! [*state cell-id _]
  (swap! *state assoc :focused-cell cell-id))

(defn submit-cell! [*state {:keys [edition] :as env} cell-id event]
  (.preventDefault event)
  (swap! *state assoc-in [:cells cell-id] (eval-cell env edition))
  (swap! *state dissoc :focused-cell)
  (swap! *state dissoc :edition))

(defn change-cell! [*state event]
  (swap! *state assoc :edition (.. event -target -value)))

(defn coll-fn [{:keys [focused-cell cells] :as env}
               {:keys [focus-cell! submit-cell! change-cell!]} l c]
  (let [cell-id (keyword (str c l))]
    ^{:key cell-id}
    [:td {:style           light-border-style
          :on-double-click (partial focus-cell! cell-id)}
     (if (= cell-id focused-cell)
       [:form {:style     {:border "1px solid #ccc"}
               :id        cell-id
               :on-submit (partial submit-cell! env cell-id)}
        [:input {:style     light-border-style
                 :type      "text"
                 :auto-focus true
                 :default-value (get cells cell-id)
                 :on-change (partial change-cell!)}]]
       (get cells cell-id))]))

(defn row-fn [cells actions-map l]
  ^{:key l}
  [:tr
   (concat
    [^{:key l}
     [:td {:style light-border-style}
      l]]
    (map (partial coll-fn cells actions-map l) az-range))])

(defn cells-ui [*cells]
  [:div {:padding "1em"}
   [:table {:style table-style}
    [:thead {:style overflow-style}
     [:tr {:style light-border-style}
      (concat [^{:key :n} [:th]]
              (map header-fn az-range))]]
    [:tbody {:style overflow-style}
     (concat [^{:key :n} [:tr (merge light-border-style overflow-style)]]
             (map (partial row-fn @*cells
                           {:focus-cell!  (partial focus-cell! *cells)
                            :submit-cell! (partial submit-cell! *cells)
                            :change-cell! (partial change-cell! *cells)}) table-lines))]]])
