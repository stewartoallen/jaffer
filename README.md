Jaffer
======

A Java implementation of Appletalk File Protocol v3.1 using TCP Transport

Disclaimer
==========

 This software is largely untested. It's barely alpha quality. Because
 OS X's AFP/TCP client appears to be full of code that does little or no
 bounds checking, you SHOULD NOT UNDER ANY CIRCUMSTANCES connect to a Java
 AFP server with a client machine that is running important services.

 YOU HAVE BEEN WARNED.


Reference documents (these appear to have been removed):

 Appletalk 3.1 Reference
 http://developer.apple.com/techpubs/macosx/Networking/AFP/AFP.pdf

 Appletalk 2.1/2.2 Reference and Appletalk over TCP
 http://developer.apple.com/techpubs/macos8/pdf/ASAppleTalkFiling2.1_2.2.pdf


Quick start
===========

 This program will act like a native Appletalk file server. You must run it
 as root to use Appletalk's normal port 548. But it's just as happy running
 on any unpriviledged port.

 Your client must be a Mac OS X 10.1 or newer system.

 Build the jar:

   ``make all``

 Run the server (sample config in doc directory):

   ``java -jar jaffer.jar -config [config-file]``

 or, alternatively, for quick testing:

   ``java -jar jaffer.jar -server [port] [volume-name] [path-to-export]``

 From the OS X Client, mount the new volume:

   ``mount_afp afp://[user]:[pass]@[host]:[port]/[volume-name] [mount-point]``

 The '[user]:[pass]@' part is optional.


Developers
==========

 Most of your work will most likely be done in the AFP_Session class
 implementing additional AFP calls. When you do this, please be mindful
 of comments in the code. They are sparse, but very important. When you
 add call implementations, please make sure they are represented in the
 AFP_Constants file and annotated with the page number in the AFP3.1
 reference PDF.

 Most of the implementation is geared towards AFP3.1 which means that some
 of the helper and common methods are not usable for AFP2.3 or earlier
 protocols.

 A JNI shared library is used to access unix authentication and process
 information. This has only been ported/tested under Linux and OS X. Further,
 in order to use shadow passwords on Linux, the server *must* run as root.


Licenses
========

 The Java AFP code is covered under the terms of the 'License' file found
 in this directory. Included in the source tree you will also find a copy
 of Stangeberry's Java Rendezvous code. This is covered under the terms of
 the GNU Lesser General Public License. A copy of this license can be found
 in the Java Rendezvous source tree.


Contact
=======

 Please contact me <stewart@neuron.com> with any questions, comments or bugs.

