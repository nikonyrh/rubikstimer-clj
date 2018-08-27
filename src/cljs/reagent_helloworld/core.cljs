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
   :start-ts    (atom 0)
   :stop-ts     (atom 0)
   :timer-str   (atom "00:00:00.000")
   :times       (atom [])})

(defn deref-state [] (zipmap (keys state) (map deref (vals state))))

(let [{:keys [timer-state start-ts stop-ts timer-str times]} state]
  (defn tick! []
    (when (= @timer-state :started)
      (reset! stop-ts (reagent-helloworld.util/nanoTime))))
  
  (let [tick-handle (atom nil)]
    (defn btn-click! []
      (let [ts (reagent-helloworld.util/nanoTime)]
        (case @timer-state
          :stopped
          (do
            (reset! start-ts ts)
            (reset! stop-ts  ts)
            (reset! timer-state :started)
            (reset! tick-handle (js/setInterval tick! 7)))
          
          :started
          (do (reset! timer-state :stopped)
              (reset! stop-ts ts)
              (swap!  times conj (- ts @start-ts))
              (js/clearInterval @tick-handle)
              (reset! tick-handle nil))))))
  
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
  (deref-state)
  
  (btn-click!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn home-page []
  [:div
   [:input {:type "button" :value @(:timer-str state) :on-click btn-click!
            :style {:padding 10 :font-size "4em" :margin-top 30}}]])

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
