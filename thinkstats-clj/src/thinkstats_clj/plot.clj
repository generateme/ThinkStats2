(ns thinkstats-clj.plot
  (:require [cljplot.render :as r]
            [cljplot.build :as b]
            [cljplot.common :refer :all]
            [clojure2d.color :as c]
            [fastmath.core :as m]
            [fastmath.random :as rnd]
            [cljplot.core :refer :all]
            [java-time :as dt]
            [fastmath.stats :as stats]))

(defn- basic-chart
  ([series {:keys [xlabel ylabel width height x y]
            :or {xlabel "x" ylabel "y" width 600 height 400}}]
   (let [s (b/preprocess-series series)
         s (if (:domain x) (b/update-scale s :x :domain (:domain x)) s)
         s (if (:fmt x) (b/update-scale s :x :fmt (:fmt x)) s)
         s (if (:fmt y) (b/update-scale s :y :fmt (:fmt y)) s)]
     (-> s
         (b/add-axes :left)
         (b/add-axes :bottom)
         (b/add-label :bottom xlabel)
         (b/add-label :left ylabel)
         (r/render-lattice {:width width :height height})))))

(defn histogram
  ([data] (histogram data {}))
  ([data config]
   (basic-chart (b/series [:grid] [:histogram data config]) config)))

(defn vbar
  ([data] (vbar data {}))
  ([data config]
   (basic-chart (b/series [:grid] [:stack-vertical [:bar data] config]) config)))

(defn histogram-discrete
  ([data] (histogram-discrete data {}))
  ([data config]
   (let [[mn mx] (stats/extent data)
         nhist (into (sorted-map) (reduce #(if-not (contains? %1 %2)
                                             (assoc %1 %2 0) %1) (frequencies data) (map int (range mn (inc mx)))))]
     (vbar nhist config))))
