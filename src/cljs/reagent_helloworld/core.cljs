(ns reagent-helloworld.core
    (:require [reagent-helloworld.util]
              
              [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

(comment
  (require '(reagent-helloworld.util)))

(defn to-fixed [len i]
  (let [i (str i)]
    (str (apply str (repeat (- len (count i)) 0))
         i)))

(defn ns-to-str [tot]
  (let [tot (quot tot 1000000)
        h   (quot tot 3600000) tot (- tot (* h 3600000))
        m   (quot tot   60000) tot (- tot (* m   60000))
        s   (quot tot    1000)  ms (- tot (* s    1000))]
    (apply str (interleave
                 (map to-fixed [2 2 2 3] [h m s ms])
                 [":" ":" "." ""]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def timer-states
  {:timer-state (atom :stopped)
   :down-ts     (atom 0)
   :start-ts    (atom 0)
   :stop-ts     (atom 0)})

(def result-states
  {:times       (atom [])
   :min-time    (atom nil)})


(defn deref-states []
  (->> (for [s [timer-states result-states]]
         (zipmap (keys s) (map deref (vals s))))
       (apply merge)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(let [{:keys [timer-state down-ts start-ts stop-ts times]} timer-states
      {:keys [times min-time]} result-states]
  (defn tick! []
    (when (= @timer-state :started)
      (reset! stop-ts (reagent-helloworld.util/nanoTime))))
  
  
  (defn conj-time! [ts]
    (reset! timer-state :stopped)
    (reset! stop-ts ts)
    (let [took-ns (- ts @start-ts)]
      (swap! times conj took-ns)
      (swap! min-time #(if % (min % took-ns) took-ns))))
  
  
  (let [tick-handle  (atom nil)
        reset-handle (atom nil)]
    (defn click! [event]
      ; (js/console.log (str @timer-state " " event))
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
                        (doseq [a [down-ts start-ts stop-ts]] (reset! a 0))) 1000)
                   (reset! reset-handle)))
            
          [:pending :up]
          (if (> @down-ts 0)
            (do (reset! timer-state :stopped))
            (do (reset! start-ts ts)
                (reset! stop-ts  ts)
                (reset! timer-state :started)
                (reset! tick-handle (js/setInterval tick! 7))))
            
          [:started :down]
          (do (conj-time! ts)
              (js/clearInterval @tick-handle)
              (reset! tick-handle nil)))))))


(comment
  (deref-state))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(let [{:keys [timer-state down-ts start-ts stop-ts]} timer-states

      events (atom (list "")) click! #(do (swap! events conj [% (reagent-helloworld.util/nanoTime)])
                                          (click! %))

      padding "60px" spacebar? #(= (.-which %) 32)
      make-event-fn #(fn[e] (when (% e) (click! %2)))
      on-mouse-down (make-event-fn identity  :down)
      on-mouse-up   (make-event-fn identity  :up)
      on-key-down   (make-event-fn spacebar? :down)
      on-key-up     (make-event-fn spacebar? :up)]

  (defn home-page []
    [:div {:tab-index 0
           :on-mouse-down on-mouse-down :on-mouse-up on-mouse-up
           :on-key-down   on-key-down   :on-key-up   on-key-up
           :touchstart    #(click! :down) :touchend #(click! :up)
           :style {:text-align "center" :class "test" :border "4px solid #8DF" :user-select "none"
                   :padding-top padding :padding-bottom padding :margin padding
                   :color (cond (= @timer-state :pending) (if (= @down-ts 0) "green" "red") :else "black")}}
     [:h1 (str (deref-states))]
     [:h1 (str (clojure.string/join "/" (take 3 @events)))]
     [:h1 (str @timer-state " " (ns-to-str (- @stop-ts @start-ts)))]]))


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
