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
  [parsed-exp]
  (-> parsed-exp
      (str/upper-case)
      (str/split #":")
      range-cells-get))

(def filling #{"of" "and"})

(defn eval-cell
  [{:keys [cells chain] :or {chain #{}} :as env} s]
  (cond
    (get chain s) "Circular dependency found!"
    (nil? s) ""
    (valid/numeric? s) s
    (is-op? (str/lower-case s)) (get kw->op (keyword (str/lower-case s)))
    (can-parse-numeric? s) (js/parseFloat s)
    (is-cell? env s) (recur (assoc env :chain (conj chain s))
                       (->> s
                            str/upper-case
                            keyword
                            (#(get cells % 0))))
    (is-range-cells? env s) (->> (parse-range-cells s)
                                 (map (partial eval-cell env))
                                 (map parse-float-if))
    (and (string? s) (str/ends-with? s "=")) (let [result (->> (str/split s #" ")
                                                               butlast
                                                               (map str/lower-case)
                                                               (map (partial eval-cell env))
                                                               flatten
                                                               (remove filling))]
                                               (if (second result)
                                                 (eval-string (str result))
                                                 (parse-float-if (first result))))
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
         (assoc-in [:cells cell-id :input]
           edition)
         (dissoc :focused-cell :edition))))

(defn change-cell!
  [*state event]
  (swap! *state assoc :edition (.. event -target -value)))

(defn expression?
  [x]
  (and (string? x)
       (re-matches #"=\s*(\w+)\s*((\((.*)\)))*" x)))

(defn extract
  [input]
  (let [[_ c r] (re-matches #"([A-Z]+)(\d+)" input)]
    [c (js/parseFloat r)]))

(defn single-cell?
  [input]
  (and
    (string? input)
    (re-matches #"\s*([A-Z]+)(\d+)\s*" input)))

(defn multi-cell?
  [input]
  (and (string? input)
       (let [splices (str/split input #":")]
         (every? single-cell? splices))))

(defn operand?
  [x]
  (and (string? x)
       (get (into #{} (map name (keys kw->op))) (str/lower-case x))))

(defmulti parse :type)

(defn seq-cell?
  [input]
  (every? single-cell? (str/split input ",")))

(defn reader
  [{:keys [cells chain] :or {chain #{}} :as env} id]
  (let [input (get-in cells [(keyword id) :input])
        typ   (cond
                (get chain id) :cycle-error
                (nil? input) :nil
                (valid/numeric? input) :float
                (can-parse-numeric? input) :parsable-float
                (expression? input) :expression
                (single-cell? input) :cell
                (seq-cell? input) :seq-cell
                (multi-cell? input) :multi-cell
                (operand? input) :operand
                :else :string)]
    (trampoline parse {:input input :id id :type typ :env env})))

(defmethod parse :cycle-error [x]
  (assoc x :error (str "Cyclic dependency found " (:id x))))

(defmethod parse :float [x]
  (assoc x :output (:input x)))

(defmethod parse :nil [x]
  (assoc x :output ""))

(defmethod parse :parsable-float [x]
  (assoc x :output (js/parseFloat (:input x))))

(defmethod parse :cell [{:keys [env input id]}]
  (let [chain (get env :chain #{})]
    #(reader (assoc env :chain (conj chain id)) input)))

(defn ->range
  [x y]
  [(min x y) (inc (max x y))])

(defn multi->cells
  [input]
  (let [[f l] (str/split input ":")
        [fc fr] (extract f)
        [lc lr] (extract l)]
    (for [c (apply range (->range (.charCodeAt fc) (.charCodeAt lc)))
          r (apply range (->range fr lr))]
      (str (char c) r))))

(defn parse-multiple
  [env cells]
  (for [cell cells]
    (trampoline parse (assoc env :type :cell :input cell))))

(defmethod parse :multi-cell [{:keys [input] :as x}]
  (let [cell-range (multi->cells input)]
    (parse-multiple x cell-range)))

(defmethod parse :seq-cell [{:keys [input] :as x}]
  (let [cell-range (str/split input ",")]
    (parse-multiple x cell-range)))

(defmethod parse :string [x]
  (assoc x :output (:input x)))

(defmethod parse :operand [{:keys [input] :as x}]
  (assoc x :output (get kw->op (keyword (str/lower-case input)))))

(defmethod parse :expression [{:keys [env input]}]
  (let [[_ op _ _ par] (re-matches #"=\s*(\w+)\s*((\((.*)\)))*" input)
        op-cell    (keyword (gensym))
        operand    (reader (assoc-in env [:cells op-cell :input] op) op-cell)
        tmp-cell   (keyword (gensym))
        parameters (str/replace par #"=\s*(\w+)\s*((\((.*)\)))*"
                                #(let [p (reader (assoc-in env [:cells tmp-cell :input] (first %1)) tmp-cell)]
                                   (map :output p)))]

    (->> operand
         (conj (for [p (str/split parameters ",")
                     :let [tmp-cell (keyword (gensym))]]
                 (reader (assoc-in env [:cells tmp-cell :input] p) tmp-cell)))
         flatten)))

(defn render
  [exp]
  (cond
    (:error exp) (:error exp)
    (and (seq? exp) (some :error exp)) (str/join ", " (mapv :error (filter :error exp)))
    (and (seq? exp) (= :operand (get (first exp) :type))) (eval-string (str (map #(if (= :string  (:type %))
                                                                                    (eval-string (:output %))
                                                                                    (:output %)) exp)))
    (seq? exp) (str/join ", " (mapv :output exp))
    :else (:output exp)))

(render (reader {:chain #{}
                 :cells {:A1 {:input "1"}}} "A1"))

(render  (reader {:chain #{}
                  :cells {:A1 {:input "A"}}} "A1"))

(render (reader {:chain #{}
                 :cells {:A2 {:input "A1"}}} "A1"))

(render (reader {:chain #{}
                 :cells {:A1 {:input "A1"}}} "A1"))

(render (reader {:chain #{}
                 :cells {:A1 {:input "A2:A3"}
                         :A2 {:input "2"}
                         :A3 {:input "3"}}} "A1"))

(render (reader {:chain #{}
                 :cells {:A1 {:input "A2,A3"}
                         :A2 {:input "2"}
                         :A3 {:input "3"}}} "A1"))

(render (reader {:chain #{}
                 :cells {:A1 {:input "A1:A3"}
                         :A2 {:input "2"}
                         :A3 {:input "A3"}}} "A1"))

(render (reader {:chain #{}
                 :cells {:A1 {:input "A1,A2"}
                         :A2 {:input "2"}}} "A1"))

(render (reader {:chain #{}
                 :cells {:A1 {:input "= Add (A2:A5)"}
                         :A3 {:input "5"}
                         :A2 {:input "6"}}} "A1"))

(render (reader {:chain #{}
                 :cells {:A1 {:input "= Add (A3,= mul (2,A2))"}
                         :A3 {:input "5"}
                         :A2 {:input "6"}}} "A1"))

(render (reader {:chain #{}
                 :cells {:A1 {:input "A3:A2"}
                         :A3 {:input "5"}
                         :A2 {:input "6"}}} "A1"))

(render (reader {:chain #{}
                 :cells {:A1 {:input "= add (A2,3)"}
                         :A2 {:input "2"}}} "A1"))

(defn coll-fn
  [{:keys [focused-cell cells] :as env}
   {:keys [focus-cell! submit-cell! change-cell!]} cell-width l c]
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
       (render (reader env cell-id)))]))

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
