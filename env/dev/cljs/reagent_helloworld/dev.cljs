(ns ^:figwheel-no-load reagent-helloworld.dev
  (:require
    [reagent-helloworld.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
