FROM ubuntu:14.04
ENV DEBIAN_FRONTEND noninteractive

#
# - update our repo
#
RUN apt-get -y update
RUN apt-get -y upgrade

#
# - install git
#
RUN apt-get -y install python python-pip git

#
# - install jdk8 from oracle
#
RUN apt-get install -y python-software-properties software-properties-common
RUN add-apt-repository ppa:webupd8team/java
RUN apt-get update
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
RUN apt-get install -y oracle-java8-installer

#
# - install building tools
#
RUN apt-get install make
RUN apt-get install -y unzip
RUN apt-get install -y build-essential
RUN apt-get install -y pkg-config
RUN apt-get install -y libtool
RUN apt-get install -y autoconf
RUN apt-get install -y automake

#
# - install libzmq-master
#
RUN git clone --depth 1 https://github.com/zeromq/libzmq.git
RUN cd libzmq && ./autogen.sh && ./configure && make && sudo make install && sudo ldconfig

#
# - install zeromq-3.2.3
#
RUN wget http://download.zeromq.org/zeromq-3.2.3.zip
RUN unzip zeromq-3.2.3.zip
RUN cd zeromq-3.2.3 && ./configure && make && sudo make install && sudo ldconfig

#
# - install jzmq
#
RUN git clone --depth 1 https://github.com/zeromq/jzmq.git
RUN cd jzmq && ./autogen.sh && ./configure && make && sudo make install && sudo ldconfig

#
# - set java.library.path
#
ENV LD_LIBRARY_PATH /usr/local/lib:/usr/local/share/java

#
# - remove defunct packages
#
RUN apt-get -y autoremove

#
# - copy lib files to docker default lib directory
#
RUN cp /usr/local/lib/libzmq.so.3.0.0 /lib/libzmq.so
RUN cp /usr/local/lib/libjzmq.so.0.0.0 /lib/libjzmq.so
