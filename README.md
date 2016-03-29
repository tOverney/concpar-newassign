# PubSub server
### A new assignment about bounded buffer.

This is based on an old assignment in java that we had to implement for our
Concurrency course. @tOverney implementation of [the old java assignment can be seen here](https://github.com/SonicFrog/PubSubServer).


### Key points of the idea
* Simple chat system
  * Subscribe to channels
  * Unsubscribe from channels
  * Publish to channel
* Give them the `frontend`
* Have them implement the crucial part of the PubSub server
  * The implementation of the circular buffer (put / get)
  * The concurrent map with the subscriber informations


### Authors
@jbcdnr, @tOverney