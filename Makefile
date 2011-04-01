OUTPUT = bin
SOURCES = $(shell find src -type f -name \*.java)
LIBS = lib/log4j-java1.1.jar
FLAGS = -target 1.5 -classpath $(LIBS) -d $(OUTPUT)
KEYSTORE_ALIAS = "dev"
KEYSTORE_PASS = "123456"

all: sign

clean:
	@(rm -rf $(OUTPUT);	mkdir $(OUTPUT))

keymap:
	@(cd src/com/tigervnc/;./keymap-gen.py)

build: keymap
	@(javac $(FLAGS) $(SOURCES))

unjarlibs:
	cd bin; find ../lib -type f -name \*.jar -exec jar xfv {} \;
	rm -rf bin/META-INF

jar: clean build unjarlibs
	@(jar cmf manifest.mf vnc.jar -C bin/ .)

sign: jar
	@(jarsigner -storepass $(KEYSTORE_PASS) -signedjar vncs.jar vnc.jar $(KEYSTORE_ALIAS))
	@(mv vncs.jar vnc.jar)

deploy: sign
	@(mkdir -p examples)
	@(cp vnc.jar examples)

runserver: deploy
	@(cd ./examples/; python -m SimpleHTTPServer)
