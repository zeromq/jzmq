This is the Java language binding for 0MQ.

To build you need to have the zeromq library already installed and run:

```bash
./autogen.sh
./configure
make
```


To build Debian package run:
```bash
$ dpkg-buildpackage -rfakeroot
```
To build RPM package run:
```bash
$ rpmbuild -tb jzmq-X.Y.Z.tar.gz
```

Where X.Y.Z is replaced with the version that you've downloaded.

If your zmq installation is not found by configure you can add this information manually e.g. `--with-zeromq=/usr/local`.

You may want to take a look at http://www.zeromq.org/docs:tuning-zeromq for additional hints.

For more information, refer to the 0MQ website at http://www.zeromq.org/.

On Mac OS X you may need to compile and make install pkg-config if configure fails with "syntax error near unexpected token newline".   
See http://stackoverflow.com/questions/3522248/how-do-i-compile-jzmq-for-zeromq-on-osx for details.   
You may also need to symlink the header files of your standard Java installation (e.g. `/Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Headers/*.h`) into a suitable directory (e.g. `/usr/local/include`) and point the `JAVA_HOME` environment variable to the parent directory (e.g.`/usr/local`).

Copying
-------

Free use of this software is granted under the terms of the GNU Lesser General
Public License (LGPL). For details see the files `COPYING` and `COPYING.LESSER`
included with the Java binding for 0MQ.
