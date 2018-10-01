(ns rubikstimer-clj.util)

(defn nanoTime []
  #?(:clj  (System/nanoTime)
     :cljs (let [d (js/Date.)] (-> (+ (.getTime d) -1500000000000 (* (.getMilliseconds d) 0.001)) (* 1000000)))))
