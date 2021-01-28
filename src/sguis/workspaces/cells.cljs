(ns sguis.workspaces.cells
  (:require
    [clojure.string :as str]
    [reagent.core :as r]
    [sci.core :refer [eval-string]]
    [sguis.workspaces.validator :as valid]))

(def cells-start
  {:focused-cell nil
   :edition      ""
   :cells        {}
   :columns      10
   :rows         5})

(defn az-range
  [columns]
  (map char (take columns (range 65 91))))

(defn table-lines
  [rows]
  (take rows (range 0 100)))

(defn possible-cells
  [{:keys [rows columns]}]
  (set (for [s (az-range columns)
             n (table-lines rows)]
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

(defn can-parse-numeric?
  [parsed-exp]
  (and (re-matches #"^[+-]?\d+(\.\d+)?$" parsed-exp)
       (valid/numeric? (js/parseFloat parsed-exp))))

(defn is-cell?
  [env parsed-exp]
  (and (= 2 (count parsed-exp))
       (get (possible-cells env) (keyword (str/upper-case parsed-exp)))))

(defn is-range-cells?
  [env parsed-exp]
  (and (= 5 (count parsed-exp))
       (->> (str/split parsed-exp #":")
            (map (comp boolean (possible-cells env) keyword str/upper-case))
            (every? true?))))

(defn range-cells-get
  [[fst snd]]
  (let [[collmin min] (name fst)
        [collmax max] (name snd)]
    (for [collv (range (.charCodeAt collmin) (inc (.charCodeAt collmax)))
          v     (range (int min) (inc (int max)))]
      (keyword (str (char collv) v)))))

(defn is-op?
  [parsed-exp]
  (contains? (set (keys kw->op)) (keyword parsed-exp)))

(defn parse-cell
  [{:keys [cells]} parsed-exp]
  (->> parsed-exp
       str/upper-case
       keyword
       (#(get cells % 0))
       js/parseFloat))

(defn parse-range-cells
  [{:keys [cells]} parsed-exp]
  (map (comp js/parseFloat #(get cells % 0) keyword)
       (-> parsed-exp
           (str/upper-case)
           (str/split #":")
           range-cells-get)))

(defn tokenizer
  [env parsed-exp]
  (cond
    (can-parse-numeric? parsed-exp) (js/parseFloat parsed-exp)
    (is-cell? env parsed-exp) (parse-cell env parsed-exp)
    (is-range-cells? env parsed-exp) (parse-range-cells env parsed-exp)
    (is-op? parsed-exp) (get kw->op (keyword parsed-exp))))

(defn parse
  [env s]
  (some->> (str/split s #" ")
           (map (partial tokenizer env))
           (remove nil?)
           flatten
           str))

(defn eval-cell
  [env s]
  (let [low-cased (some-> s str/lower-case)]
    (cond (nil? s) ""
          (str/ends-with? low-cased "=") (some-> (parse env low-cased)
                                                 (eval-string {:allow (vals kw->op)})
                                                 str)
          :else s)))

;; UI impl

(def overflow-style
  {:overflow "auto"})

(defn light-border-style
  [width]
  {:border  "1px solid #ccc"
   :width   width
   :padding "0.5em"})

(defn header-fn
  [width chars]
  ^{:key chars}
  [:td {:style (light-border-style width)} chars])

(defn focus-cell!
  [*state cell-id _]
  (swap! *state assoc :focused-cell cell-id))

(defn submit-cell!
  [*state {:keys [edition]}
   cell-id
   event]
  (.preventDefault event)
  (swap! *state
    #(-> %
         (assoc-in [:cells cell-id]
           (if (str/ends-with? edition "=")
             (str/lower-case edition)
             edition))
         (dissoc :focused-cell :edition))))

(defn change-cell!
  [*state event]
  (swap! *state assoc :edition (.. event -target -value)))

(defn coll-fn
  [{:keys [focused-cell cells cell-width] :as env}
   {:keys [focus-cell! submit-cell! change-cell!]} l c]
  (let [cell-id (keyword (str c l))]
    ^{:key cell-id}
    [:td {:style           (light-border-style cell-width)
          :data-testid     cell-id
          :on-double-click (partial focus-cell! cell-id)}
     (if (= cell-id focused-cell)
       [:form {:style       {:border "1px solid #ccc"}
               :id          cell-id
               :data-testid (str "form-" (name cell-id))
               :on-submit   (partial submit-cell! env cell-id)}
        [:input {:style         (light-border-style cell-width)
                 :type          "text"
                 :data-testid   (str "input-" (name cell-id))
                 :auto-focus    true
                 :default-value (get cells cell-id)
                 :on-change     (partial change-cell!)}]]
       (eval-cell env (get cells cell-id)))]))

(defn row-fn
  [cells actions-map cell-width l]
  ^{:key l}
  [:tr
   (concat
     [^{:key l}
      [:td {:style (light-border-style 42)} l]
      (map (partial coll-fn cells actions-map l) (az-range (:columns cells)))])])

(defn change-width
  [state]
  (swap! state assoc :window-width (* 0.9 (.-innerWidth js/window))))

(defn cells-ui
  ([]
   (r/with-let [*cells (r/atom cells-start)]
     [cells-ui *cells]))
  ([*cells]
   (.addEventListener js/window "resize" #(change-width *cells))
   (change-width *cells)
   (let [width      (:window-width @*cells)
         columns    (:columns @*cells)
         cell-width (/ width columns)]
     [:div.panel.is-primary
      {:style {:margin "auto"
               :width  width}}
      [:div.panel-heading {:style {:width width}} "Spreadsheets"]
      [:div {:style {:width    width
                     :height   (* 0.5 (.-innerHeight js/window))
                     :overflow :scroll}}
       [:table {:id          "table"
                :data-testid "table"}
        [:thead {:style       overflow-style
                 :data-testid "thead"}
         [:tr {:style (light-border-style cell-width)}
          (concat [^{:key :n} [:th]]
                  (map (partial header-fn cell-width) (az-range (:columns @*cells)))
                  [^{:key "btn-col"} [:th
                                      [:button.button.is-primary
                                       {:on-click #(swap! *cells update :columns (partial (fn [x] (min (inc x) 26))))}
                                       "Add column"]]])]]
        [:tbody {:style       overflow-style
                 :data-testid "tbody"}
         (concat [^{:key :n} [:tr (merge (light-border-style cell-width) overflow-style)]]
                 (map (partial row-fn @*cells
                               {:focus-cell!  (partial focus-cell! *cells)
                                :submit-cell! (partial submit-cell! *cells)
                                :change-cell! (partial change-cell! *cells)}
                               cell-width) (table-lines (:rows @*cells)))
                 [^{:key "btn-row"}
                  [:tr [:td [:button.button.is-primary
                             {:on-click #(swap! *cells update :rows (partial (fn [x] (min (inc x) 100))))}
                             "Add row"]]]])]]]])))

