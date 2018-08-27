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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def state
  {:timer-state (atom :stopped)
   :down-ts     (atom 0)
   :start-ts    (atom 0)
   :stop-ts     (atom 0)
   :timer-str   (atom "00:00:00.000")
   :times       (atom [])})

(defn deref-state [] (zipmap (keys state) (map deref (vals state))))

(let [{:keys [timer-state down-ts start-ts stop-ts timer-str times]} state]
  (defn tick! []
    (when (= @timer-state :started)
      (reset! stop-ts (reagent-helloworld.util/nanoTime))))
  
  (let [tick-handle  (atom nil)
        reset-handle (atom nil)]
    (defn click! [event]
      (js/console.log (str @timer-state " " event))
      (let [ts (reagent-helloworld.util/nanoTime)]
        (case [@timer-state event]
          [:stopped :mouse-down]
          (do (reset! down-ts ts)
              (reset! timer-state :pending)
              (when @reset-handle
                (js/clearTimeout @reset-handle)
                (reset! reset-handle nil))
              (->> (js/setTimeout
                     #(when (= @timer-state :pending)
                        (doseq [a [down-ts start-ts stop-ts]] (reset! a 0))) 1000)
                   (reset! reset-handle)))
          
          [:stopped :mouse-up] nil
          
          [:pending :mouse-up]
          (if (> @down-ts 0)
            (do (reset! timer-state :stopped))
            (do (reset! start-ts ts)
                (reset! stop-ts  ts)
                (reset! timer-state :started)
                (reset! tick-handle (js/setInterval tick! 7))))
          
          [:started :mouse-down]
          (do (reset! timer-state :stopped)
              (reset! stop-ts ts)
              (swap!  times conj (- ts @start-ts))
              (js/clearInterval @tick-handle)
              (reset! tick-handle nil))
          
          [:started :mouse-up] nil))))
  
  (add-watch start-ts :start-ts-watch (fn [k r o n] (reset! stop-ts n)))
  (add-watch stop-ts  :stop-ts-watch
    (fn [k r o n]
      (let [tot (quot (- @stop-ts @start-ts) 1000000)
            h   (quot tot 3600000) tot (- tot (* h 3600000))
            m   (quot tot   60000) tot (- tot (* m   60000))
            s   (quot tot    1000)  ms (- tot (* s    1000))
            [h m s ms] (map to-fixed [2 2 2 3] [h m s ms])]
        (reset! timer-str (str h ":" m ":" s "." ms))))))


(comment
  (deref-state))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(let [{:keys [timer-state down-ts start-ts timer-str]} state
      padding "60px"]
  (defn home-page []
    [:div {:on-mouse-down #(click! :mouse-down)
           :on-mouse-up   #(click! :mouse-up)
           :style {:text-align "center" :class "test" :border "1px solid #8DF" :user-select "none";
                   :padding-top padding :padding-bottom padding :margin padding
                   :color (cond (= @timer-state :pending) (if (= @down-ts 0) "green" "red") :else "black")}}
     [:h1 @timer-str]]))


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
