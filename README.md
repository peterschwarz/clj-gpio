[![Build Status](https://travis-ci.org/peterschwarz/clj-gpio.svg)](https://travis-ci.org/peterschwarz/clj-gpio)

# clj-gpio

A basic library for reading, writing and watching GPIO signals on a Raspberry
Pi, in a Clojure REPL-friendly way.

## Usage

Add the following to your `project.clj`

    [clj-gpio 0.1.1-SNAPSHOT]

Fire up a REPL, and require `gpio.core`.

### GPIO Read/Write 

We can open a basic read/write gpio port as follows (let's say we have an LED
conncted to GPIO 17):

    user=> (require '[gpio.core :refer :all] :reload)
    nil
    user=> (def port (open-port 17))
    #'user/port

To read the value of the port, we can do the following:

    user=> (read-value port)
    :low

Or, more conveniently, we can deref it:

    user=> @port
    :low

To set values on the port, The port needs to be configured for `out` mode:

    user=> (set-direction! port :out)

This also works with `'out` and `"out"`.  A value can be written to the port
as follows:

    user=> (write-value! port :high)

With our LED connected to gpio 17, we should see it turned on.  We can also
read back the value and see that `(= :high @port)`.

### GPIO Listening.

We can also pull events off of a gpio port by using `open-channel-port`.  In
addition to setting directions, values etc, we set the edge change that we'll
listen for, and we can create a `core.async` channel from which can receive
values. 

For example (if we have a push button on GPIO 18):

    user=> (def ch-port (open-channel-port 18))
    #'user/ch-port
    user=> (set-direction! ch-port :in)
    nil
    user=> (set-edge! ch-port :both) ; or :falling, :rising, and :none to disable 
    nil

 We'll also set the bit to :high when the button pressed:

    user=> (set-active-low! ch-port true) 
    nil

Let's turn on the LED we defined in the Read/Write example above when our
button is pressed: 

    user=> (def ch (create-edge-channel ch-port))
    #'user/ch
    user=>  (require '[clojure.core.async :as a :refer [go <!]])
    nil
    user=> (go (loop []
             (when-let [value (<! ch)]
                (write-value! port value)
                (recur))))
    #<ManyToManyChannel clojure.core.async.impl.channels.ManyToManyChannel@1197ad0>


When we're finished with the channel, we call:

     user=> (a/close! ch)
     nil

And clean up our ports:

    user=> (close! port)
    nil
    user=> (close! ch-port)
    nil

## Development

First compile the java sources:

    lein javac

then fire up your REPL and require `gpio.core` as usual.

Note, the edge channel will only operate on the Raspberry PI platform.

## License

Copyright © 2014 Peter Schwarz

Distributed under the Eclipse Public License, the same as Clojure.
