(ns gpio.core
  (:require [clojure.core.async :as a :refer [go <! >! >!! chan sliding-buffer tap]])
  (:import [java.io RandomAccessFile FileOutputStream PrintStream]
           [java.nio.channels FileChannel FileChannel$MapMode]
           [io.bicycle.epoll EventPolling EventPoller PollEvent]))

(defn export! [port]
  (spit "/sys/class/gpio/export" (str port)))

(defn unexport! [port]
  (spit "/sys/class/gpio/unexport" (str port)))

(defn- do-set-direction! [port direction]
  {:pre [(some #(= direction %) [:in :out 'in 'out "in" "out"])]}
  (spit (str "/sys/class/gpio/gpio" port "/direction") (name direction)))

(defn- do-set-edge! [port setting]
  {:pre [(some #(= setting %) [:none, :falling, :rising, :both,
                               'none, 'falling, 'rising, 'both
                               "none", "falling", "rising","both"])]}

  (spit (str "/sys/class/gpio/gpio" port "/edge") (name setting)))


(defn- do-set-active-low [port-num active-low?]
  (spit (str "/sys/class/gpio/gpio" port-num "/active_low") (if active-low? "1" "0")))

(defn high-low-value [value]
  {:pre [(some #(= value %) [:high :low 1 0 'high 'low "1" "0" \1 \0])]}
  (byte (condp = value
          :high \1
          1     \1
          'high \1
          "1"   \1
          :low  \0
          0     \0
          'low  \0
          "0"   \0
          value)))

(defprotocol Closeable
  (close! [self] "Closes this object"))

(defprotocol GpioPort
  (set-direction! [port direction] "Sets the direction of this port: in or out.")
  (set-active-low! [port active-low?] "Invert the logic of the value pin for both reading and writing so that a high == 0 and low == 1. ")
  (read-value [port] "Return the value of the port")
  (write-value! [port value] "Writes the value to the port.  The value may be specified as :high, :low (and symbol or string variations), \1, \0, or 1, 0"))

(defprotocol GpioChannelProvider
  (set-edge! [providor setting])
  (create-edge-channel [port] "Returns a core async channel on which events will be put")
  (release-edge-channel! [port channel]))

(defn- value-file [port]
  (str "/sys/class/gpio/gpio" port "/value"))

(defn- random-access [filename]
  (RandomAccessFile. filename "rw"))

(defn open-port
  "Opens a port from which values may be read or written.
  Args:
  * port - the gpio pin number
  * opts: a map of optional properts
      * :direction - sets the initial direction
      * :active-low? - sets the port as \"active low\", value true or false
      * :initial-value - sets the intitial value of the pin.
      * :from-raw-fn - a converter function, which takes the `char`
          value (\1 or \0) read and converts it into a meaningful value.
          The default converts the values to :high and :low respectively "
  [port & [opts]]
  (export! port)
  (let [{:keys [from-raw-fn direction active-low? initial-value]
         :or {from-raw-fn #(if (= \1 %) :high :low)}} opts
        filename (value-file port)
        raf (random-access filename)
        props {:port port, :file-name filename, :file raf}
        gpio-port (reify
                   clojure.lang.ILookup

                   (valAt [_ k not-found] (get props k not-found))
                   (valAt [_ k] (k props))

                   GpioPort

                   (set-direction! [_ direction] (do-set-direction! port direction))

                   (set-active-low! [_ active-low?] (do-set-active-low port active-low?))

                   (read-value
                    [_]
                    (.seek raf 0)
                    (from-raw-fn (char (.read raf))))

                   (write-value!
                    [_ value]
                    (.seek raf 0)
                    (.writeByte raf (high-low-value value)))

                   Closeable
                   (close! [_]
                           (.close raf)
                           (unexport! port)))]
    (try
      (when direction (set-direction! gpio-port direction))
      (when active-low? (set-active-low! gpio-port active-low?))
      (when initial-value (write-value! gpio-port initial-value))
      gpio-port
      (catch Exception e
        (close! gpio-port)
        (throw e)))

))

(defn open-channel-port
  "Opens a port which can be used for listening to value changes.  In addition to the
  options map for a basic port, the following are available:
  * :event-buffer-size - the size of the change events buffer.  Defaults to 1
  * :timeout - sets a timeout for the OS polling wait time.  Defaults to -1, which waits indefinitely
  * :edge - sets the edge value- :rising, :falling, or :both"
  [port & [opts]]
  (let [{:keys [event-buffer-size timeout edge]
         :or {event-buffer-size 1, timeout -1}} opts
        create-channel (fn [] (chan (sliding-buffer event-buffer-size)))
        gpio-port (open-port port opts)
        poller (EventPolling/create)
        write-ch (chan 1)
        read-ch (create-channel)
        mult-ch (a/mult read-ch)]

    (.addFile poller (:file gpio-port) (bit-or EventPolling/EPOLLIN EventPolling/EPOLLET EventPolling/EPOLLPRI) gpio-port)

    (when edge (do-set-edge! port edge))

    ; Serialize the write loop
    (go (loop []
          (when-let [data (<! write-ch)]
            (write-value! gpio-port data)
            (recur))))

    (go (loop []
          ; TODO: Let's use a timeout for more predictable shutdowns
          (when-let [events (.poll poller timeout)]
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
