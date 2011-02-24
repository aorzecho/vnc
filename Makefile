OUTPUT = bin
SOURCES = $(shell find src -type f -name \*.java)
LIBS = lib/log4j-java1.1.jar
FLAGS = -target 1.5 -classpath $(LIBS) -d $(OUTPUT)
KEYSTORE_ALIAS = "dev"
KEYSTORE_PASS = "123456"

all: sign

keymap:
	@(cd src/com/tigervnc/vncviewer;./keymap-gen.py)

build: keymap
	@(javac $(FLAGS) $(SOURCES))

unjarlibs:
	cd bin; find ../lib -type f -name \*.jar -exec jar xfv {} \;
	rm -rf bin/META-INF

jar: build unjarlibs
	@(jar cmf manifest.mf vnc.jar -C bin/ .)

sign: jar
	@(jarsigner -storepass $(KEYSTORE_PASS) -signedjar vncs.jar vnc.jar $(KEYSTORE_ALIAS))
	@(mv vncs.jar vnc.jar)

deploy: sign
	@(mkdir -p examples/applet)
	@(cp vnc.jar examples/applet/)

runserver: deploy
	@(python ./examples/dev_appserver.py)
