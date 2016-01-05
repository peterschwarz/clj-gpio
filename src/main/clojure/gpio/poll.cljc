(ns gpio.poll
  #?(:clj (:require [clojure.core.async :as a :refer [go-loop]]
                    [clojure.core.async.impl.protocols :as p])
     :cljs (:require [cljs.nodejs :as nodejs]))
  #?(:clj (:import [io.bicycle.epoll EventPolling EventPoller PollEvent])))

#?(
:clj (do

  (def ^:private POLLING_CONFIG
    (bit-or EventPolling/EPOLLIN EventPolling/EPOLLET EventPolling/EPOLLPRI))

  (defn watch-port [port ^Integer timeout on-change-fn]
    (let [poller (EventPolling/create)]
      (.addFile poller (:filename port) POLLING_CONFIG port)
      
      (go-loop []
        (when-let [events (.poll poller timeout)]
          (doseq [_ (filter #(= port (.getData %)) events)]
            (on-change-fn))
          (recur)))

      poller))

  (defn cancel-watch [^EventPoller poller]
    (.close poller))
       
)

:cljs (do

  (def ^:private fs (nodejs/require "fs"))

  (defn watch-port [port _ on-change-fn]
    (.watch fs (:filename port)
            (fn [event filename]
              (when (and (= "change" event))
                (on-change-fn)))))

  (defn cancel-watch [poller]
    (.close poller))
        
))
