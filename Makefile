OUTPUT = bin
SOURCES = $(shell find src/main -type f -name \*.java)
LIBS = lib/log4j-java1.1.jar:lib/plugin.jar
FLAGS = -target 1.5 -classpath $(LIBS) -d $(OUTPUT)
KEYSTORE_ALIAS = "dev"
KEYSTORE_PASS = "123456"

all: deploy

clean:
	@(rm -rf $(OUTPUT);	mkdir $(OUTPUT))

keymap:
	@(cd src/main/java/com/tigervnc/;./keymap-gen.py)

build: keymap
	@(javac $(FLAGS) $(SOURCES))

unjarlibs:
	cd bin; \
	find ../lib -type f -name \*.jar \
      -not -name plugin.jar -exec jar xfv {} \;
	rm -rf bin/META-INF

jar: clean build unjarlibs
	@(jar cmf manifest.mf vnc.jar -C bin/ .)

sign: jar
	@(jarsigner -storepass $(KEYSTORE_PASS) -signedjar vncs.jar vnc.jar $(KEYSTORE_ALIAS))
	@(jarsigner -storepass $(KEYSTORE_PASS) -signedjar lib/log4j-java1.1.signed.jar lib/log4j-java1.1.jar $(KEYSTORE_ALIAS))
	@(mv vncs.jar vnc.jar)

deploy: sign
	@(mkdir -p examples/js/vnc/resources)
	@(mv lib/log4j-java1.1.signed.jar examples/js/vnc/resources/log4j-java1.1.jar)
	@(cp vnc.jar examples/js/vnc/resources)

runserver: deploy
	@(cd ./examples/; python -m SimpleHTTPServer)
