(ns map-reduce.master
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]))

(declare mapper combiner sum reducer)

(defn -main [& args]
  (with-open [rdr (clojure.java.io/reader "Jabberwocky.txt")]
    (->> (doall (line-seq rdr))
         (map mapper)
         (combiner)
         (reducer)
         (sort)
         (pp/pprint))))

(defn mapper [line]
  (map #(vector % 1) (str/split (str/lower-case line) #"\W")))

(defn combiner [mapped]
  (->> (apply concat mapped)
       (group-by first)
       (map (fn [[k v]]
              {k (map second v)}))))

(defn sum [word]
  {(apply key word) (apply + (apply val word))})

(defn reducer [collected-values]
  (apply merge (map sum collected-values)))