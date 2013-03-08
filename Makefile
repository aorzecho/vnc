OUTPUT = bin
RESOURCES = src/main/resources
SOURCES = $(shell find src/main -type f -name \*.java)
LIBS = lib/plugin.jar
FLAGS = -source 1.5 -target 1.5 -classpath $(LIBS) -d $(OUTPUT)
KEYSTORE_ALIAS = "dev"
KEYSTORE_PASS = "123456"

all: deploy

clean:
	@(rm -rf $(OUTPUT);	mkdir $(OUTPUT))

build: 
	@(javac $(FLAGS) $(SOURCES))

copyres:
	@(cp -r $(RESOURCES)/* $(OUTPUT))

unjarlibs:
	cd bin; \
	find ../lib -type f -name \*.jar \
      -not -name plugin.jar -exec jar xfv {} \;
	rm -rf bin/META-INF

jar: clean build copyres unjarlibs
	@(jar cmf manifest.mf vnc.jar -C bin/ .)

sign: jar
	@(jarsigner -storepass $(KEYSTORE_PASS) -signedjar vncs.jar vnc.jar $(KEYSTORE_ALIAS))
	@(mv vncs.jar vnc.jar)

deploy: sign
	@(mkdir -p examples/js/vnc/resources)
	@(cp vnc.jar examples/js/vnc/resources)

runserver: deploy
	@(cd ./examples/; python -m SimpleHTTPServer)
