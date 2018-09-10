(ns reagent-helloworld.core
    (:require [reagent-helloworld.util]
              
              [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

(comment
  (require '(reagent-helloworld.util)))

(defn to-fixed [len i]
  (let [i (str i)]
    (str (clojure.string/join (repeat (- len (count i)) 0))
         i)))

(defn ns-to-str [tot]
  (let [tot (quot tot 1000000)
      ; h   (quot tot 3600000) tot (- tot (* h 3600000))
      ; m   (quot tot   60000) tot (- tot (* m   60000))
        s   (quot tot    1000)  ms (- tot (* s    1000))]
    (apply str (interleave
                 (map to-fixed [2 2] [s (quot ms 10)])
                 ["." ""]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defonce timer-states
  {:timer-state (atom :stopped)
   :down-ts     (atom 0)
   :start-ts    (atom 0)
   :stop-ts     (atom 0)})

(defonce result-states
  {:times       (atom (vec (for [_ (range 15)] (-> (Math/random) (* 5) (+ 20) (* 1e9) long))))
   :min-time    (atom nil)
   :result-avgs (atom {5 nil 12 nil})})

(defn deref-states []
  (->> (for [s [timer-states result-states]]
         (zipmap (keys s) (map deref (vals s))))
       (apply merge)))

(defn avg-of-n [results n]
  (let [n-tot (count results)]
    (when (>= n-tot n)
      (->> (subvec results (- n-tot n) n-tot) sort rest butlast (apply +) double (* (/ (- n 2)))))))

(comment
  (let [data [-10 2 30 4 5]]
    (for [i [3 5 10 12]]
      [i (avg-of-n data i)])))

(def min-took-ns 2e9)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(let [{:keys [timer-state down-ts start-ts stop-ts times]} timer-states
      {:keys [times min-time result-avgs]} result-states]
  (defn tick! []
    (when (= @timer-state :started)
      (reset! stop-ts (reagent-helloworld.util/nanoTime))))
  
  
  (defn conj-time! [ts]
    (reset! timer-state :stopped)
    (reset! stop-ts ts)
    (let [took-ns (- ts @start-ts)]
      (when (> took-ns min-took-ns)
        (swap! min-time #(if % (min % took-ns) took-ns))
        (let [times  (swap! times conj took-ns)
              avgs   @result-avgs
              n-avgs (keys avgs)]
          (reset! result-avgs
            (zipmap n-avgs (for [n n-avgs] (avg-of-n times n))))))))
  
  
  (let [tick-handle  (atom nil)
        reset-handle (atom nil)]
    (defn click! [event]
      (js/console.log (str @timer-state " " event))
      (let [ts (reagent-helloworld.util/nanoTime)]
        (case [@timer-state event]
          [:stopped :up]   nil ; Mouse up when we stop the clock
          [:pending :down] nil ; Holding down a spacebar
          
          [:stopped :down]
          (do (reset! down-ts ts)
              (reset! timer-state :pending)
              (when @reset-handle
                (js/clearTimeout @reset-handle)
                (reset! reset-handle nil))
              (->> (js/setTimeout
                     #(when (= @timer-state :pending)
                        (doseq [a [down-ts start-ts stop-ts]] (reset! a 0))) 333)
                   (reset! reset-handle)))
          
          [:pending :up]
          (if (> @down-ts 0)
            (do (reset! timer-state :stopped))
            (do (reset! start-ts ts)
                (reset! stop-ts  ts)
                (reset! timer-state :started)
                (reset! tick-handle (js/setInterval tick! 7))))
          
          [:started :down]
          (do
            (js/clearInterval @tick-handle)
            (reset! tick-handle nil)
            (conj-time! ts)))))))


(comment
  (deref-states))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(let [{:keys [timer-state down-ts start-ts stop-ts]} timer-states
      {:keys [times min-time result-avgs]} result-states

      spacebar? #(= (.-which %) 32)
      make-event-fn #(fn[e] (when (% e) (click! %2)))
      on-mouse-down (make-event-fn identity  :down)
      on-mouse-up   (make-event-fn identity  :up)
      on-key-down   (make-event-fn spacebar? :down)
      on-key-up     (make-event-fn spacebar? :up)]
  
  (defn home-page []
    (let [time-ns (- @stop-ts @start-ts)]
      [:div
       [:div {:id "app-sidebar"}
        [:ul (for [[ix t] (map list (range) @times)]
               ^{:key ix} [:li (ns-to-str t)])]]
       
       [:div {:tab-index 0 :id "app-timer"
              :on-mouse-down  on-mouse-down :on-mouse-up  on-mouse-up
              :on-touch-start on-mouse-down :on-touch-end on-mouse-up
              :on-key-down    on-key-down   :on-key-up    on-key-up
              :style {:color (cond
                               (= @timer-state :pending) (if (= @down-ts 0) "green" "red")
                               (= @timer-state :stopped) (if (> time-ns min-took-ns) "black" "gray")
                               :else "black")}}
        [:h1 (ns-to-str time-ns)]]])))
     


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn mount-root []
  (reagent/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
