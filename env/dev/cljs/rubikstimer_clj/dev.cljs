(ns ^:figwheel-no-load rubikstimer-clj.dev
  (:require
    [rubikstimer-clj.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
