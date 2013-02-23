# Changelog

* Remove assert where runtime errors are more appropriate
* Use utf8 when compiling java
* Add unit test for REQ/REP send more
* Update comment on ZDispatcher to warn users about busy spin wait strategy
* Fix regression where compatibility with zmq 2.1.X was broken by zero copy

## v2.1.1 - February 16, 2013

* Add zero copy API to send and recv
* Remove asserts from get_context JNI
* Add ZLoop support
* Poller rewrite
* No longer c assert when trying to write to a closed Socket
* Add a continuous integration support through travis-ci

## v2.1.0 - February 12, 2013

* First release to Maven Central
