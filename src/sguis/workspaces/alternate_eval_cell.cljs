(ns sguis.workspaces.alternate-eval-cell
  "Sipmle and easy cell evaluation"
  (:require [clojure.string :as str]
            [sci.core :refer [eval-string]]
            [sguis.workspaces.validator :as valid]))

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
  "Tests if parameter string is able to parse numeric"
  [parsed-exp]
  (and (string? parsed-exp)
       (re-matches #"^\s*[+-]?\d+(\.\d+)?\s*$" parsed-exp)
       (valid/numeric? (js/parseFloat parsed-exp))))

(defn parse-float-if
  "Parses string to float, if it's parsable"
  [s]
  (if (can-parse-numeric? s)
    (js/parseFloat s)
    s))

(def xp-matcher
  "Regex for expression detection and extraction"
  #"=?\s*(\w+)\s*\(*([\w\d,\.\:]+){1}\)")

(defn expression?
  "Expression validation"
  [x]
  (and (string? x)
       (first (re-seq xp-matcher x))))

(defn extract
  "Extracts column and row from cell name"
  [input]
  (let [[_ c r] (re-matches #"([A-Z]+)(\d+)" input)]
    [c (js/parseFloat r)]))

(defn single-cell?
  "True if input is referring to a single cell"
  [input]
  (and
    (string? input)
    (re-matches #"\s*([A-Z]+)(\d+)\s*" input)))

(defn multi-cell?
  "True if input is referring to multiple cells like A1:B2"
  [input]
  (and (string? input)
       (let [splices (str/split input #":")]
         (every? single-cell? splices))))

(defn operand?
  "returns true if the parameter is an operand"
  [x]
  (and (string? x)
       (get (into #{} (map name (keys kw->op))) (str/lower-case x))))

(defn seq-cell?
  "True if input is referring to sequence of cells like A1,B2"
  [input]
  (every? single-cell? (str/split input ",")))

(defmulti evaluate :type)

(defn switch
  "Trail switch, for decide how the referred cell should be evaluated"
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
    #(evaluate {:input input :id id :type typ :env env})))

(defmethod evaluate :cycle-error [{:keys [id] :as m}]
  ;;Evaluate cyclic dependency
  (assoc m :error (str "Cyclic dependency found " id)))

(defmethod evaluate :float [{:keys [input] :as m}]
  ;;Evaluate float number
  (assoc m :output input))

(defmethod evaluate :nil [x]
  ;;Evaluate nil
  (assoc x :output ""))

(defmethod evaluate :parsable-float [{:keys [input] :as m}]
  ;;Evaluate string as float
  (assoc m :output (js/parseFloat input)))

(defmethod evaluate :cell [{:keys [env input id]}]
  ;;Evaluating cell's content
  (let [chain (get env :chain #{})]
    #(switch (assoc env :chain (conj chain id)) input)))

(defn ->range
  "Create valid range of two values"
  [x y]
  [(min x y) (inc (max x y))])

(defn multi->seq
  "Generate cell sequence from multi-cell format
  from A1:B2 -> [A1, A2, B1, B2]"
  [input]
  (let [[f l] (str/split input ":")
        [fc fr] (extract f)
        [lc lr] (extract l)]
    (for [c (apply range (->range (.charCodeAt fc) (.charCodeAt lc)))
          r (apply range (->range fr lr))]
      (str (char c) r))))

(defn parse-multiple
  "Parsing sequence of cells, one by one"
  [env cells]
  (for [cell cells]
    (trampoline evaluate (assoc env :type :cell :input cell))))

(defmethod evaluate :multi-cell [{:keys [input] :as m}]
  ;;Evaluate multi cells like A1:B2
  (let [cell-range (multi->seq input)]
    (parse-multiple m cell-range)))

(defmethod evaluate
  ;;Evaluation of cell sequence like A1,A2,A3
  :seq-cell
  [{:keys [input] :as m}]
  (let [cell-range (str/split input ",")]
    (parse-multiple m cell-range)))

(defmethod evaluate :string [{:keys [input] :as m}]
  ;;Evaluate string to itself
  (assoc m :output input))

(defmethod evaluate :operand [{:keys [input] :as m}]
  ;;Evaluate operand of expression
  (assoc m :output (get kw->op (keyword (str/lower-case input)))))

(declare eval-cell)

(defmethod evaluate :expression [{:keys [env input]}]
  ;;Recursive evaluation of an expression.
  ;;It's replaces the smallest sub-expression with it's calculated value, while the whole expression has been resolved.
  ;;The sub-expressions are evaluated in temporary cells
  (let [[ex op par] (first (re-seq xp-matcher input))
        op-cell     (keyword (gensym))
        operand     (:output (trampoline switch (assoc-in env [:cells op-cell :input] op) op-cell))
        parsed-pars (for [p (str/split par ",")
                          :let [tmp-cell (keyword (gensym))
                                value (eval-cell (assoc-in env [:cells tmp-cell :input] p) tmp-cell)]]
                      value)
        result      (eval-string (str (flatten [operand (if (every? string? parsed-pars)
                                                          (map parse-float-if (str/split (first parsed-pars) ","))
                                                          parsed-pars)])))
        next-call (str/replace input ex result)
        next-cell   (keyword (gensym))]
    (trampoline switch (assoc-in env [:cells next-cell :input] next-call) next-cell)))

(defn render
  "Final rendering of a given expression"
  [exp]
  (cond
    (:error exp) (:error exp)
    (and (seq? exp) (some :error exp)) (str/join ", " (mapv :error (filter :error exp)))
    (seq? exp) (str/join ", " (mapv :output exp))
    :else (:output exp)))

(defn eval-cell
  "Evaluate a cell in it's environment"
  [env id]
  (-> (trampoline switch env id)
      render))
