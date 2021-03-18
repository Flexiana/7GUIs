(ns sguis.workspaces.alternate-cells
  "7GUIs cells UI"
  (:require
    [sguis.workspaces.alternate-eval-cell :refer [eval-cell]]
    [reagent.core :as r]))

(def cells-start
  "Initial state"
  {:focused-cell nil
   :edition      ""
   :cells        {}
   :columns      10
   :rows         5})

(defn az-range
  "Columns from A-Z (or less)"
  [columns]
  (map char (take columns (range 65 91))))

(defn table-lines
  "Rows from 0-100 (or less)"
  [rows]
  (take rows (range 0 100)))


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
  "Change focus"
  [*state cell-id _]
  (swap! *state assoc
    :focused-cell cell-id
    :edition (get-in @*state [:cells cell-id :input])))

(defn submit-cell!
  "Store cells input"
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
  "Update input on change"
  [*state event]
  (swap! *state assoc :edition (.. event -target -value)))

(defn cell-fn
  "UI representation of a cell"
  [{:keys [focused-cell edition] :as env}
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
        [:input {:style         {:margin "0px"
                                 :padding "0px"
                                 :width "100%"}
                 :type          "text"
                 :data-testid   (str "input-" (name cell-id))
                 :auto-focus    true
                 :default-value edition
                 :on-change     (partial change-cell!)}]]
       (eval-cell env cell-id))]))

(defn row-fn
  "UI representation of a row of cells"
  [cells actions-map cell-width l]
  ^{:key l}
  [:tr {:style {:display "table-row"}}
   (concat
     [^{:key l}
      [:td {:style (light-border-style 42)} l]
      (map (partial cell-fn cells actions-map cell-width l) (az-range (:columns cells)))])])

(defn change-width!
  "Set table width to the 90% of the window"
  [state]
  (swap! state assoc :window-width (* 0.9 (.-innerWidth js/window))))

(defn add-row!
  "Extend the table with one row"
  [*cells]
  (swap! *cells update :rows #(min (inc %) 100)))

(defn row-btn
  "Button for add row"
  [add-row!]
  [^{:key "btn-row"}
   [:tr
    [:td
     [:button.button.is-primary
      {:on-click add-row!}
      "Add row"]]]])

(defn add-col!
  "Extend table with one column"
  [*cells]
  (swap! *cells update :columns #(min (inc %) 26)))

(defn coll-btn
  "button for adding column"
  [add-col!]
  [^{:key "btn-col"}
   [:th
    [:button.button.is-primary
     {:on-click add-col!}
     "Add column"]]])

(defn table-head
  "Head of the table, with add column button"
  [cells
   cell-width
   add-col!]
  [:thead {:style       overflow-style
           :data-testid "thead"}
   [:tr {:style (:style       {:display "table-header-group"})}
    (concat [^{:key :n} [:th {:style (light-border-style cell-width)}]]
      (map (partial header-fn cell-width) (az-range (:columns cells)))
      (coll-btn add-col!))]])

(defn table-body
  [{:keys [rows] :as cells} cell-width actions-map add-row!]
  [:tbody {:style       {:display "table-row-group"}
           :data-testid "tbody"}
   (concat [^{:key :n} [:tr (merge (light-border-style cell-width) overflow-style)]]
     (map (partial row-fn cells actions-map cell-width)
       (table-lines rows))
     (row-btn add-row!))])

(defn alt-cells-ui
  "Main UI of cells"
  ([]
   (r/with-let [*cells (r/atom cells-start)]
     [alt-cells-ui *cells]))
  ([*cells]
   (.addEventListener js/window "resize" (partial change-width! *cells))
   (change-width! *cells)
   [:div.panel.is-primary
    {:style {:margin "auto"
             :width  "100%"}}
    [:div.panel-heading "Alternative Spreadsheets"]
    [:div {:style {:height   (* 0.5 (.-innerHeight js/window))
                   :overflow "scroll"}}
     [:table {:id          "table"
              :data-testid "table"
              :style       {:width        "100%"
                            :table-layout "fixed"
                            :max-width    "80vw"}}
      [table-head @*cells "10rem" (partial add-col! *cells)]
      [table-body @*cells "10rem" {:focus-cell!  (partial focus-cell! *cells)
                                   :submit-cell! (partial submit-cell! *cells)
                                   :change-cell! (partial change-cell! *cells)}
       (partial add-row! *cells)]]]]))
