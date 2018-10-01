(ns rubikstimer-clj.prod
  (:require [rubikstimer-clj.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
