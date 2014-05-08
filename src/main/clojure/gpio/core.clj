(ns gpio.core
  (:require [clojure.core.async :as a :refer [go <! >!! chan sliding-buffer tap]])
  (:import [java.io RandomAccessFile FileOutputStream PrintStream]
           [java.nio.channels FileChannel FileChannel$MapMode]
           [io.bicycle.epoll EventPolling EventPoller PollEvent]))

(defn write-to [file-name content]
  (let [printStream (PrintStream. (FileOutputStream. file-name))]
    (.print printStream content)
    (.close printStream)))

(defn export! [port]
  (write-to "/sys/class/gpio/export", (str port)))

(defn unexport! [port]
  (write-to "/sys/class/gpio/unexport", (str port)))

(defn- do-set-direction! [port direction]
  {:pre [(some #(= direction %) [:in :out 'in 'out "in" "out"])]}
  (write-to (str "/sys/class/gpio/gpio" port "/direction") (name direction)))

(defn- do-set-edge! [port setting]
  {:pre [(some #(= setting %) [:none, :falling, :rising, :both,
                               'none, 'falling, 'rising, 'both
                               "none", "falling", "rising","both"])]}

  (write-to (str "/sys/class/gpio/gpio" port "/edge") (name setting)))


(defn- do-set-active-low [port-num active-low?]
  (write-to (str "/sys/class/gpio/gpio" port-num "/active_low") (if active-low? "1" "0")))

(defn- high-low-value [value]
  {:pre [(some #(= value %) [:high :low 1 0 'high 'low "1" "0" \1 \0])]}
  (byte (condp = value
          :high \1
          1     \1
          'high \1
          :low  \0
          0     \0
          'low  \0
          value)))

(defprotocol Closeable
  (close! [self] "Closes this object"))

(defprotocol GpioPort
  (set-direction! [port direction] "Sets the direction of this port: in or out.")
  (set-active-low! [port active-low?] "Invert the logic of the value pin for both reading and writing so that a high == 0 and low == 1. ")
  (read-value [port] "Return the value of the port")
  (write-value! [port value] "Writes the value of the port"))

(defprotocol GpioChannelProvider
  (set-edge! [providor setting])
  (create-edge-channel [port] "Returns a core async channel on which events will be put")
  (release-edge-channel! [port channel]))

(defn- read-high-low [raf]
  (.seek raf 0)
  ; TODO: Add this as a configurable conversion function
  (if (= \1 (char (.read raf))) :high :low))

(defn- value-file [port]
  (str "/sys/class/gpio/gpio" port "/value"))

(defn- random-access [filename]
  (RandomAccessFile. filename "rw"))

(defn open-port [port]
  (export! port)
  (let [filename (value-file port)
        raf (random-access filename)
        props {:port port, :file-name filename, :file raf}
        ]
    (reify
      clojure.lang.ILookup

      (valAt [_ k not-found] (get props k not-found))
      (valAt [_ k] (k props))

      GpioPort

      (set-direction! [_ direction] (do-set-direction! port direction))

      (set-active-low! [_ active-low?] (do-set-active-low port active-low?))

      (read-value
       [_]
       (.seek raf 0)
       (if (= \1 (char (.read raf))) :high :low))

      (write-value!
       [_ value]
       (.seek raf 0)
       (.writeByte raf (high-low-value value)))

      Closeable
      (close! [_]
              (.close raf)
              (unexport! port)))))

(defn open-channel-port [port]
  ; TODO: buffer size
  (let [create-channel (fn [] (chan (sliding-buffer 1)))
        gpio-port (open-port port)
        poller (EventPolling/create)
        write-ch (chan 1)
        read-ch (create-channel)
        mult-ch (a/mult read-ch)]

    (.addFile poller (:file gpio-port) (bit-or EventPolling/EPOLLIN EventPolling/EPOLLET EventPolling/EPOLLPRI) gpio-port)

    ; Serialize the write loop
    (go (loop []
          (when-let [data (<! write-ch)]
            (write-value! gpio-port data)
            (recur))))

    (go (loop []
          ; TODO: Let's use a timeout for more predictable shutdowns
          (when-let [events (.poll poller -1)]
            (doseq [_ (filter #(=  gpio-port (.getData %)) events)]
              (>! read-ch (read-value gpio-port)))
            (recur))))


    (reify
      GpioPort

      (set-direction! [_ direction] (set-direction! gpio-port direction))
      (set-active-low! [_ active-low?] (set-active-low! gpio-port active-low?))
      (read-value [_] (read-value gpio-port))

      (write-value! [this value] (>!! write-ch value))

      GpioChannelProvider

      (set-edge! [_ setting]
                 (do-set-edge! port setting))

      (create-edge-channel [_]
                           (let [ch (create-channel)]
                             (tap mult-ch ch)
                             ch))

      (release-edge-channel! [_ ch]
                            (a/untap mult-ch ch)
                            (a/close! ch))

      Closeable
      (close! [_]
              (a/close! write-ch)
              (a/close! read-ch)
              (.close poller)
              (close! gpio-port)))))
