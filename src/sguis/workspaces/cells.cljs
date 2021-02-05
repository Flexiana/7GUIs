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
       (re-matches #"^\s*[+-]?\d+(\.\d+)?\s*$" parsed-exp)
       (valid/numeric? (js/parseFloat parsed-exp))))

(defn parse-float-if
  [s]
  (if (can-parse-numeric? s)
    (js/parseFloat s)
    s))

(def xp-matcher
  #"=?\s*(\w+)\s*\(*([\w\d,\.\:]+){1}\)")

(defn expression?
  [x]
  (and (string? x)
       (first (re-seq xp-matcher x))))

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

(defn seq-cell?
  [input]
  (every? single-cell? (str/split input ",")))

(defmulti parse :type)

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
    #(parse {:input input :id id :type typ :env env})))

(defmethod parse :cycle-error [{:keys [id] :as m}]
  (assoc m :error (str "Cyclic dependency found " id)))

(defmethod parse :float [{:keys [input] :as m}]
  (assoc m :output input))

(defmethod parse :nil [x]
  (assoc x :output ""))

(defmethod parse :parsable-float [{:keys [input] :as m}]
  (assoc m :output (js/parseFloat input)))

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

(defmethod parse :multi-cell [{:keys [input] :as m}]
  (let [cell-range (multi->cells input)]
    (parse-multiple m cell-range)))

(defmethod parse :seq-cell [{:keys [input] :as m}]
  (let [cell-range (str/split input ",")]
    (parse-multiple m cell-range)))

(defmethod parse :string [{:keys [input] :as m}]
  (assoc m :output input))

(defmethod parse :operand [{:keys [input] :as m}]
  (assoc m :output (get kw->op (keyword (str/lower-case input)))))

(declare ->render)

(defmethod parse :expression [{:keys [env input]}]
  (let [[ex op par] (first (re-seq xp-matcher input))
        op-cell     (keyword (gensym))
        operand     (:output (trampoline reader (assoc-in env [:cells op-cell :input] op) op-cell))
        parsed-pars (for [p (str/split par ",")
                          :let [tmp-cell (keyword (gensym))]]
                      (->render (assoc-in env [:cells tmp-cell :input] p) tmp-cell))
        result      (eval-string (str (flatten [operand (if (every? string? parsed-pars)
                                                          (map parse-float-if (str/split (first parsed-pars) ","))
                                                          parsed-pars)])))
        next-call (str/replace input ex result)
        next-cell   (keyword (gensym))]
    (trampoline reader (assoc-in env [:cells next-cell :input] next-call) next-cell)))

(defn render
  [exp]
  (cond
    (:error exp) (:error exp)
    (and (seq? exp) (some :error exp)) (str/join ", " (mapv :error (filter :error exp)))
    (and (seq? exp) (= :operand (get (first exp) :type))) (-> (map :output exp)
                                                              str
                                                              eval-string
                                                              parse-float-if)
    (seq? exp) (str/join ", " (mapv :output exp))
    :else (:output exp)))

(defn ->render
  [env id]
  (render (trampoline reader env id)))

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
       (->render env cell-id))]))

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
