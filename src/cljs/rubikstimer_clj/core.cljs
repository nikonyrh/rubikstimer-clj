(ns rubikstimer-clj.core
    (:require [rubikstimer-clj.util]
              
              [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

; lein clean && lein figwheel
; (ns rubikstimer-clj.core)

; SKIP_BUILD=1 ./build.sh

(comment
  (require '(rubikstimer-clj.util)))

(defn to-fixed "Add leading zeros to an integer" [len i]
  (let [i (str i)]
    (-> (clojure.string/join (repeat (- len (count i)) "0"))
        (str i))))

(defn ns-to-str [tot]
  (when tot
    (let [tot (quot tot 1000000)
        ; h   (quot tot 3600000) tot (- tot (* h 3600000))
        ; m   (quot tot   60000) tot (- tot (* m   60000))
          s   (quot tot    1000)  ms (- tot (* s    1000))]
      (apply str (interleave
                   (map to-fixed [2 2] [s (quot ms 10)])
                   ["." ""])))))


(let [opposites {:U #{:D :U}, :L #{:L :R}, :F #{:F :B}, :D #{:D :U}, :R #{:L :R}, :B #{:F :B}}
      filter-fn (fn [this-side prev-side modifier]
                  (when-not ((opposites this-side) prev-side)
                    (str (name this-side) modifier)))]
  (defn generate-scramble []
    (let [side-seq     (repeatedly #(rand-nth [:B :D :F :L :R :U]))
          modifier-seq (repeatedly #(rand-nth ["" "'" "2"]))]
      (->> (map filter-fn (rest side-seq) side-seq modifier-seq)
           (filter some?)))))


; (->> (generate-scramble) (take 100))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defonce timer-states
  {:timer-state (atom :stopped)
   :down-ts     (atom 0)
   :start-ts    (atom 0)
   :stop-ts     (atom 0)
   :scramble    (atom "")})


(defonce result-states
  {:times       (atom
                  (case :empty
                    :empty  ()
                    :sample (for [_ (range 15)] (-> (Math/random) (* 5) (+ 20) (* 1e9) long))))
   :result-avgs (atom (sorted-map -1 nil 5 nil 12 nil))})

(defn deref-states []
  (->> (for [s [timer-states result-states]]
         (zipmap (keys s) (map deref (vals s))))
       (apply merge)))

(defn avg-of-n [n results]
  (let [first-n (take n results)]
    (when (-> first-n count (= n))
      (if (= n 1)
        (first first-n)
        (->> first-n sort rest butlast (apply +) double (* (/ (- n 2))))))))

(comment
  (let [data (reverse [-10 2 30 4 5])]
    (for [i [1 3 5 10 12]]
      [i (avg-of-n i data)])))


(def min-took-ns 2e9)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(let [{:keys [timer-state down-ts start-ts stop-ts times scramble]} timer-states
      {:keys [times result-avgs]} result-states]
  
  (let [scramble-seq (atom (generate-scramble))
        scramble-len 25
        drop-len     (+ scramble-len 10)] ; dropping some extra items to make sequences statistically independent
    (defn generate-scramble! []
      (->>  @scramble-seq (take scramble-len) (clojure.string/join " ") (reset! scramble))
      (swap! scramble-seq (partial drop drop-len))
      @scramble))
  
  
  (defn tick! []
    (when (= @timer-state :started)
      (reset! stop-ts (rubikstimer-clj.util/nanoTime))))
  
  
  (defn reset-result-avgs!
    ([] (some-> @times first reset-result-avgs!))
    ([took-ns]
     (let [times  @times
           avgs   @result-avgs
           n-avgs (keys avgs)]
       (->> (zipmap n-avgs
              (for [n n-avgs]
                (case n
                  -1
                  (if-let [current-best (avgs n)]
                    (min current-best took-ns)
                    took-ns)
                  
                  (avg-of-n n times))))
            (into (sorted-map))
            (reset! result-avgs)))))
  
  
  (defn conj-time! [ts]
    (reset! timer-state :stopped)
    (reset! stop-ts ts)
    (let [took-ns (- ts @start-ts)]
      (when (> took-ns min-took-ns)
        (swap! times conj took-ns)
        (reset-result-avgs! took-ns))))
  
; (def log (atom [])) (def t0 (rubikstimer-clj.util/nanoTime))
  
  (let [tick-handle  (atom nil)
        reset-handle (atom nil)]
    (defn click! [event]
    ; (js/console.log (str @timer-state " " event))
      
      (let [ts (rubikstimer-clj.util/nanoTime)]
      ; (swap! log conj [(-> ts (- t0) (* 1e-6) Math/round (* 1e-3)) ts @down-ts @stop-ts @timer-state event])
        
        (case [@timer-state event]
          [:stopped :up]   nil ; Mouse up when we stop the clock
          [:pending :down] nil ; Holding down a spacebar
          
          [:stopped :down]
          (when (-> ts (- @stop-ts) (> 0.2e9))
            (reset! down-ts ts)
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
            (do (reset! timer-state :stopped)
                (generate-scramble!))
            (do (reset! start-ts ts)
                (reset! stop-ts  ts)
                (reset! timer-state :started)
                (reset! tick-handle (js/setInterval tick! 7))))
          
          [:started :down]
          (when (-> (- ts @start-ts) (> 0.2e9))
            (js/clearInterval @tick-handle)
            (reset! tick-handle nil)
            (conj-time! ts)))))))


(comment
  (deref-states))

(defonce _ ; An init hack :/
  [(reset-result-avgs!)
   (generate-scramble!)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(let [{:keys [timer-state down-ts start-ts stop-ts scramble]} timer-states
      {:keys [times result-avgs]} result-states
      
      spacebar?     #(= (.-which %) 32)
      make-event-fn #(fn[e] (when (% e) (click! %2)))
      on-mouse-down  (make-event-fn identity  :down)
      on-mouse-up    (make-event-fn identity  :up)
      on-key-down    (make-event-fn spacebar? :down)
      on-key-up      (make-event-fn spacebar? :up)]
  
  (defn home-page []
    (let [time-ns (- @stop-ts @start-ts)]
      [:div
       [:div {:id "app-sidebar"}
        [:ul (for [[ix t] (reverse (map list (range) @times))]  ; reverse, what a hack...
               (let [clr (condp > ix 5 "800" 12 "A80" "AAA")]   ; indicate which are the most recent 5 & 12 runs
                 ^{:key (str "sidebar-li-" ix "-" clr)}
                 [:li {:style {:color (str "#" clr)}}
                  (ns-to-str t)]))]]
       
       [:div {:tab-index 0 :id "app-timer"
              :on-mouse-down  on-mouse-down :on-mouse-up  on-mouse-up
              :on-touch-start on-mouse-down :on-touch-end on-mouse-up
              :on-key-down    on-key-down   :on-key-up    on-key-up
              :style {:color (case @timer-state
                               :pending (if (= @down-ts 0) "green" "red")
                               :stopped (if (or (= time-ns 0)
                                                (> time-ns min-took-ns)) "black" "gray")
                               "lightgray")}}
        [:h1 (ns-to-str time-ns)]
        [:h2 @scramble]]
       
       [:div {:id "app-bottombar"}
        (for [[k v] @result-avgs]
          (let [id (str "app-bottombar-" (Math/abs k))]
            ^{:key (str id "-" v)}
            [:span {:id id}
             (case k -1 "BEST" (str "AVG " k)) " "
             [:b (-> v ns-to-str (or "-"))]]))]])))



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
