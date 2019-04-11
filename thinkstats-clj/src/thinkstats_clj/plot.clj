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
         s (if (:domain y) (b/update-scale s :y :domain (:domain y)) s)
         s (if (:fmt x) (b/update-scale s :x :fmt (:fmt x)) s)
         s (if (:fmt y) (b/update-scale s :y :fmt (:fmt y)) s)
         s (if (:ticks x) (b/update-scale s :x :ticks (:ticks x)) s)
         s (if (:ticks y) (b/update-scale s :y :ticks (:ticks y)) s)]
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
         counts (reduce #(if-not (contains? %1 %2)
                           (assoc %1 %2 0) %1) (frequencies data) (map int (range mn (inc mx))))
         nhist (into (sorted-map) (if (:percents? config)
                                    (let [total (stats/sum data)]
                                      (map (fn [[k v]] [k (/ v total)]) counts))
                                    counts))]
     (vbar nhist config))))

(defn- infer-pmf
  [data config]
  (if (sequential? (first data))
    (b/add-multi (b/series [:grid]) :function (map-indexed #(vector %1 (partial rnd/pdf
                                                                                (rnd/distribution :enumerated-int {:data %2}))) data) config {:color (c/palette-presets :category10)})
    (b/series [:grid] [:function
                       (partial rnd/pdf (rnd/distribution :enumerated-int {:data data})) config])))

(defn pmf-int
  ([data] (pmf data {}))
  ([data config]
   (let [config (if (contains? config :domain)
                  config
                  (assoc config :domain (take 2 (stats/extent data))))]
     (basic-chart (infer-pmf data config) config))))

(defn functions
  ([data] (functions data {}))
  ([data config]
   (basic-chart (b/add-multi (b/series [:grid]) :function data config {:color (map #(c/set-alpha % 200) (c/palette-presets :category10))}) config)))

(defn function
  ([data] (function data {}))
  ([data config]
   (basic-chart (b/series [:grid] [:function data config]) config)))


(defn lines
  ([data] (lines data {}))
  ([data config]
   (basic-chart (b/add-multi (b/series [:grid]) :line data config {:color (map #(c/set-alpha % 160) (c/palette-presets :category10))}) config)))


(defn cdfs
  ([data] (cdfs data {}))
  ([data config]
   (basic-chart (b/add-multi (b/series [:grid]) :cdf data config {:color (map #(c/set-alpha % 160) (c/palette-presets :category10))}) config)))

(defn cdf
  ([data] (cdf data {}))
  ([data config]
   (basic-chart (b/series [:grid] [:cdf data config]) config)))
