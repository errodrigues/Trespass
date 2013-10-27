Trespass
========

A simple, unobtrusive and yet powerful Java library that allows on-the-fly inspection of classes' and objects' contents at runtime, regardless of declared visibility.

The implementation is based on [Java dynamic proxies] (http://docs.oracle.com/javase/7/docs/api/java/lang/reflect/Proxy.html) but that's a detail that the library will keep hidden from consumers.

Using Trespass will basically involve 2 components:

1. a custom interface extending `trespass.Trespasser` and declaring methods that will map to constructors, methods and/or properties, static or not, in a target class or object

2. class `trespass.Factory` to dynamically create objects that will implement the declared interface and proxy calls to its target
