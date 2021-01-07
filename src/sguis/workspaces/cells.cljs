(ns sguis.workspaces.cells
  (:require [reagent.core :as r]
            [clojure.string :as str]))

(def *cells
  (r/atom {:focused-cell nil
           :edition      ""
           :cells        {}}))

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

(def a->z
  (map char (range 65 91)))

(def table-lines
  (range 0 100))

(def possible-cells
  (set (for [s a->z
             n table-lines]
         (keyword (str s n)))))

(defn header-fn [chars]
  ^{:key chars}
  [:td {:style light-border-style} chars])

(defn focus-cell! [*state cell-id _]
  (swap! *state assoc :focused-cell cell-id))

(defn submit-cell! [*state cell-id edition event]
  (.preventDefault event)
  (swap! *state assoc-in [:cells cell-id] edition)
  (swap! *state dissoc :focused-cell)
  (swap! *state dissoc :edition))

(defn change-cell! [*state event]
  (swap! *state assoc :edition (.. event -target -value)))

(defn coll-fn [{:keys [focused-cell cells edition]}
               {:keys [focus-cell! submit-cell! change-cell!]} l c]
  (let [cell-id (keyword (str l c))]
    ^{:key cell-id}
    [:td {:style           light-border-style
          :on-double-click (partial focus-cell! cell-id)}
     (if (= cell-id focused-cell)
       [:form {:style     {:border "1px solid #ccc"}
               :id        cell-id
               :on-submit (partial submit-cell! cell-id edition)}
        [:input {:style     light-border-style
                 :type      "text"
                 :on-change change-cell!}]]
       (get cells cell-id))]))

(defn row-fn [cells actions-map l]
  ^{:key l}
  [:tr
   (concat
    [^{:key l}
     [:td {:style light-border-style}
      l]]
    (map (partial coll-fn cells actions-map l) a->z))])

(defn cells-ui [*cells]
  [:div {:padding "1em"}
   [:table {:style table-style}
    [:thead {:style overflow-style}
     [:tr {:style light-border-style}
      (concat [^{:key :n} [:th]]
              (map header-fn a->z))]]
    [:tbody {:style overflow-style}
     (concat [^{:key :n} [:tr (merge light-border-style overflow-style)]]
             (map (partial row-fn @*cells
                           {:focus-cell!  (partial focus-cell! *cells)
                            :submit-cell! (partial submit-cell! *cells)
                            :change-cell! (partial change-cell! *cells)}) table-lines))]]])

(defn numeric? [x]
  (and (number? x) (not (js/Number.isNaN x))))

(def kw->op
  {:add  #(+ %1 %2)
   :sub  #(- %1 %2)
   :div  #(/ %1 %2)
   :mul  #(* %1 %2)
   :mod  #(mod %1 %2)
   :sum  +
   :prod *})

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

(defn is-op? [parsed-exp]
  ((set (keys kw->op)) (keyword parsed-exp)))

(defn tokenizer [parsed-exp]
  (cond
    (can-parse-numeric? parsed-exp) (js/parseFloat parsed-exp)
    (is-cell? parsed-exp)           (keyword (str/upper-case parsed-exp))
    (is-range-cells? parsed-exp)    (map keyword (-> parsed-exp
                                                     (str/upper-case)
                                                     (str/split #":")))
    (is-op? parsed-exp)             (get kw->op (keyword parsed-exp))))

(defn parse [s]
  (let [parsed (-> s
                   str/lower-case)]
    (cond (str/ends-with? parsed "=") (->> (str/split parsed #" ")
                                           (map tokenizer)
                                           (remove nil?))
          :else                       s)))

(defn list-of-kw? [ls]
  (and (list? ls)
       (every? keyword? ls)))

(defn range-cells-get [kw-range]
  (let [[fst snd]     kw-range
        [collmin min] (name fst)
        [collmax max] (name snd)]
    (for [collv (range (.charCodeAt collmin) (inc (.charCodeAt collmax)))
          v     (range (int min) (inc (int max)))]
      (keyword (str (char collv) v)))))

(defn cells-parse-data [cells tokenized-exp]
  (reduce (fn [acc v]
            (conj acc (cond (list-of-kw? v) (map #(get cells % 0) v) ;; eval every cell between
                            #_#_            (keyword? v)             (get cells v)
                            #_#_:else       v))) [] tokenized-exp))

#_(cells-parse-data {:A2 2
                     :B8 8} (parse "Sum of A2:B8 ="))
#_(cells-parse-data {:A2 2
                     :B8 8} `(~+ (:A2 :B8)))

#_(is (= `(~+ (:A2 :B8)) (parse "Sum of A2:B8 ="))
      (= `(~+ 4.0 (:A2 :B8)) (parse "Sum of 4 and A2:B8 ="))
      (= `(~/ :B5 :C5) (parse "Div of B5 and C5 ="))
      (= `(2) (parse "2 =")))
#_(is (= :A3 (tokenizer "A3"))
      (= [:A3 :B5] (tokenizer "A3:B5"))
      (= + (tokenize "sum")))
