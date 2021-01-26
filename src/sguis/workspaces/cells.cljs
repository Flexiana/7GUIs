(ns sguis.workspaces.cells
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sci.core :refer [eval-string]]
            [sguis.workspaces.validator :as valid]))

(def cells-start
  {:focused-cell nil
   :edition      ""
   :cells        {}})

(def az-range
  (map char (range 65 91)))

(def table-lines
  (range 0 100))

(def possible-cells
  (set (for [s az-range
             n table-lines]
         (keyword (str s n)))))

;; Parser Impl

(def kw->op
  {:add      `+
   :subtract `-
   :div      `/
   :mul      `*
   :mod      `mod
   :sum      `+
   :prod     `*})

(defn can-parse-numeric? [parsed-exp]
  (and (re-matches #"^[+-]?\d+(\.\d+)?$" parsed-exp)
    (valid/numeric? (js/parseFloat parsed-exp))))

(defn is-cell? [parsed-exp]
  (and (= 2 (count parsed-exp))
    (possible-cells (keyword (str/upper-case parsed-exp)))))

(defn is-range-cells? [parsed-exp]
  (and (= 5 (count parsed-exp))
    (->> (str/split parsed-exp #":")
      (map (comp boolean possible-cells keyword str/upper-case))
      (every? true?))))

(defn range-cells-get [[fst snd]]
  (let [[collmin min] (name fst)
        [collmax max] (name snd)]
    (for [collv (range (.charCodeAt collmin) (inc (.charCodeAt collmax)))
          v (range (int min) (inc (int max)))]
      (keyword (str (char collv) v)))))

(defn is-op? [parsed-exp]
  (contains? (set (keys kw->op)) (keyword parsed-exp)))

(defn parse-cell [{:keys [cells]} parsed-exp]
  (->> parsed-exp
    str/upper-case
    keyword
    (#(get cells % 0))
    js/parseFloat))

(defn parse-range-cells [{:keys [cells]} parsed-exp]
  (map (comp js/parseFloat #(get cells % 0) keyword)
    (-> parsed-exp
      (str/upper-case)
      (str/split #":")
      range-cells-get)))

(defn tokenizer [env parsed-exp]
  (cond
    (can-parse-numeric? parsed-exp) (js/parseFloat parsed-exp)
    (is-cell? parsed-exp) (parse-cell env parsed-exp)
    (is-range-cells? parsed-exp) (parse-range-cells env parsed-exp)
    (is-op? parsed-exp) (get kw->op (keyword parsed-exp))))

(defn parse [env s]
  (some->> (str/split s #" ")
    (map (partial tokenizer env))
    (remove nil?)
    flatten
    str))

(defn eval-cell [env s]
  (let [low-cased (some-> s str/lower-case)]
    (cond (nil? s) ""
          (str/ends-with? low-cased "=") (some-> (parse env low-cased)
                                           (eval-string {:allow (vals kw->op)})
                                           str)
          :else s)))

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

(defn submit-cell! [*state {:keys [edition]} cell-id event]
  (.preventDefault event)
  (swap! *state assoc-in [:cells cell-id] edition)
  (swap! *state dissoc :focused-cell)
  (swap! *state dissoc :edition))

(defn change-cell! [*state event]
  (swap! *state assoc :edition (.. event -target -value)))

(defn coll-fn [{:keys [focused-cell cells] :as env}
               {:keys [focus-cell! submit-cell! change-cell!]} l c]
  (let [cell-id (keyword (str c l))]
    ^{:key cell-id}
    [:td {:style           light-border-style
          :data-testid cell-id
          :on-double-click (partial focus-cell! cell-id)}
     (if (= cell-id focused-cell)
       [:form {:style     {:border "1px solid #ccc"}
               :id        cell-id
               :data-testid (str "form_" cell-id)
               :on-submit (partial submit-cell! env cell-id)}
        [:input {:style         light-border-style
                 :type          "text"
                 :data-testid   (str "input_" cell-id)
                 :auto-focus    true
                 :default-value (get cells cell-id)
                 :on-change     (partial change-cell!)}]]
       (eval-cell env (get cells cell-id)))]))

(defn row-fn [cells actions-map l]
  ^{:key l}
  [:tr
   (concat
     [^{:key l}
      [:td {:style light-border-style}
       l]]
     (map (partial coll-fn cells actions-map l) az-range))])

(defn cells-ui
  ([]
   (r/with-let [*cells (r/atom cells-start)]
     [cells-ui *cells]))
  ([*cells]
   [:div {:padding "1em"}
    [:table {:style table-style
             :data-testid "table"}
     [:thead {:style overflow-style
              :data-testid "thead"}
      [:tr {:style light-border-style}
       (concat [^{:key :n} [:th]]
         (map header-fn az-range))]]
     [:tbody {:style overflow-style
              :data-testid "tbody"}
      (concat [^{:key :n} [:tr (merge light-border-style overflow-style)]]
        (map (partial row-fn @*cells
               {:focus-cell!  (partial focus-cell! *cells)
                :submit-cell! (partial submit-cell! *cells)
                :change-cell! (partial change-cell! *cells)}) table-lines))]]]))
