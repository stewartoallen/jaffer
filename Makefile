# settings you may need to change

PLATFORM=$(shell uname -s)
JDKDIR=/opt/java/jdk/1.3.1

# nothing beyond this line should need to be changed

VERSION=0.07
BASENAME=jaffer
FULLNAME=${BASENAME}-${VERSION}
LIBDIR=${JDKDIR}/jre/bin
LIBOUT=obj/lib
INCDIR=src/lib
CSOURCE=src/lib/OS_Server.c

# build rules

all: lib-${PLATFORM} jar

setup:
	mkdir -p obj/lib

javac: setup
	find src -name "*.java" | xargs javac -O -d obj

jar: javac
	cd obj && jar -cfm ../${BASENAME}.jar ../src/MANIFEST com lib

lib-Linux: setup ${CSOURCE}
	gcc -shared -o ${LIBOUT}/libLinux.so ${CSOURCE} -I${INCDIR} -L${LIBDIR} -ljava -lcrypt

lib-Darwin: setup ${CSOURCE}
	gcc -fPIC -dynamic -bundle \
	-o ${LIBOUT}/libMacOSX.so ${CSOURCE} -I${INCDIR} \
	-I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers \
	-L/System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Libraries \
	-ljvm -lpthread 

lib-SunOS: setup ${CSOURCE}
	gcc -shared -o ${LIBOUT}/libSunOS.so ${CSOURCE} -I${INCDIR} \
	-I/usr/java/include -I/usr/java/include\solaris \
	-L/usr/java/jre/lib -L/usr/java/jre/lib/sparc \
	-ljava -lcrypt

srcball: clean jar
	mkdir -p web jaffer-${VERSION} && \
	cp -r [LRM]* jaffer.jar src jaffer-${VERSION} && \
	cp -r jaffer.jar web/${FULLNAME}.jar && \
	tar --exclude .svn -zcf web/${FULLNAME}-src.tgz jaffer-${VERSION} && \
	ln -sf ${FULLNAME}.jar web/${BASENAME}.jar && \
	ln -sf ${FULLNAME}-src.tgz web/${BASENAME}-src.tgz && \
	rm -rf jaffer-${VERSION}

dist: srcball all
	cp ${BASENAME}.jar web/${FULLNAME}.jar && \
	ln -sf ${FULLNAME}.jar web/${BASENAME}.jar

dist-sf: srcball make
	cd web && sf-dist ${FULLNAME}.jar ${FULLNAME}-src.tgz

remake: clean all

clean:
	rm -rf obj/com *.jar

