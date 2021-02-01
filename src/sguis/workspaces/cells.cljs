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
  (and (string? parsed-exp)
       (re-matches #"^[+-]?\d+(\.\d+)?$" parsed-exp)
       (valid/numeric? (js/parseFloat parsed-exp))))

(defn is-cell?
  [env parsed-exp]
  (when parsed-exp
    (and (string? parsed-exp)
         (= 2 (count parsed-exp))
         (get (possible-cells env) (keyword (str/upper-case parsed-exp))))))

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
      (str (char collv) v))))

(defn is-op?
  [parsed-exp]
  (contains? (set (keys kw->op)) (keyword parsed-exp)))

(defn parse-float-if
  [s]
  (if (can-parse-numeric? s)
    (js/parseFloat s)
    s))

(declare eval-cell)

(defn parse-range-cells
  [env parsed-exp]
  (let [r (-> parsed-exp
              (str/upper-case)
              (str/split #":")
              range-cells-get)]
    (->> r
         (map (partial eval-cell env))
         (map parse-float-if))))

(defn tokenizer
  [env parsed-exp]
  (cond
    (can-parse-numeric? parsed-exp) (js/parseFloat parsed-exp)
    (is-cell? env parsed-exp) (eval-cell env parsed-exp)
    (is-range-cells? env parsed-exp) (parse-range-cells env parsed-exp)
    (is-op? parsed-exp) (get kw->op (keyword parsed-exp))))

(defn eval-cell
  [{:keys [cells chain] :as env} s]
  (cond
    (get chain s) "Circular dependency found!"
    (nil? s) ""
    (can-parse-numeric? s) s
    (is-cell? env s) (eval-cell (assoc env :chain (conj chain s))
                                (->> s
                                     str/upper-case
                                     keyword
                                     (#(get cells % 0))))
    (is-op? s) (get kw->op (keyword s))
    (and (string? s) (str/ends-with? s "=")) (let [tokenized (->>
                                                               (str/split (str/lower-case s) #" ")
                                                               (map (partial tokenizer env))
                                                               (remove nil?)
                                                               (map parse-float-if)
                                                               flatten)]
                                               (str (if (valid/numeric? (first tokenized))
                                                      (apply str tokenized)
                                                      (eval-string (str tokenized)))))
    :else s))

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
           (if
             (str/ends-with? edition "=")
             (str/lower-case edition)
             edition))
         (dissoc :focused-cell :edition))))

(defn change-cell!
  [*state event]
  (swap! *state assoc :edition (.. event -target -value)))

(defn coll-fn
  [{:keys [focused-cell cells] :as env}
   {:keys [focus-cell! submit-cell! change-cell!]} cell-width l  c]
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
       (eval-cell (assoc env :chain #{}) (get cells cell-id)))]))

#_:clj-kondo/ignore

(defn row-fn
  [cells actions-map cell-width l]
  ^{:key l}
  [:tr
   (concat
     [^{:key l}
      [:td {:style (light-border-style 42)} l]
      (map (partial coll-fn cells actions-map cell-width l) (az-range (:columns cells)))])])

(defn change-width!
  [state]
  (.addEventListener
    js/window "resize"
    (swap! state assoc :window-width (* 0.9 (.-innerWidth js/window)))))

(defn add-row!
  [*cells]
  (swap! *cells update :rows #(min (inc %) 100)))

(defn row-btn
  [add-row!]
  [^{:key "btn-row"}
   [:tr
    [:td
     [:button.button.is-primary
      {:on-click add-row!}
      "Add row"]]]])

(defn add-col!
  [*cells]
  (swap! *cells update :columns #(min (inc %) 26)))

(defn coll-btn
  [add-col!]
  [^{:key "btn-col"}
   [:th
    [:button.button.is-primary
     {:on-click add-col!}
     "Add column"]]])

(defn table-head
  [cells
   cell-width
   add-col!]
  [:thead {:style       overflow-style
           :data-testid "thead"}
   [:tr {:style (light-border-style cell-width)}
    (concat [^{:key :n} [:th]]
            (map (partial header-fn cell-width) (az-range (:columns cells)))
            (coll-btn add-col!))]])

(defn table-body
  [{:keys [rows] :as cells} cell-width actions-map add-row!]
  [:tbody {:style       overflow-style
           :data-testid "tbody"}
   (concat [^{:key :n} [:tr (merge (light-border-style cell-width) overflow-style)]]
           (map (partial row-fn cells actions-map cell-width)
                (table-lines rows))
           (row-btn add-row!))])

(defn cells-ui
  ([]
   (r/with-let [*cells (r/atom cells-start)]
     [cells-ui *cells]))
  ([*cells]
   (change-width! *cells)
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
        [table-head @*cells cell-width (partial add-col! *cells)]
        [table-body @*cells cell-width {:focus-cell!  (partial focus-cell! *cells)
                                        :submit-cell! (partial submit-cell! *cells)
                                        :change-cell! (partial change-cell! *cells)}
         (partial add-row! *cells)]]]])))
