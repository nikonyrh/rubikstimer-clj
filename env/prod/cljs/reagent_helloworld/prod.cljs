(ns reagent-helloworld.prod
  (:require [reagent-helloworld.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
