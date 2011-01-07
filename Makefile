OUTPUT = bin
SOURCES = $(shell find src -type f -name \*.java)
FLAGS = -target 1.5 -d $(OUTPUT)
KEYSTORE_ALIAS = "dev"
KEYSTORE_PASS = "123456"

all: sign

keymap:
	@(cd src/com/tigervnc/vncviewer;./keymap-gen.py)

build: keymap
	@(javac $(FLAGS) $(SOURCES))

jar: build
	@(jar cmf manifest.mf vnc.jar -C bin/ .)

sign: jar
	@(jarsigner -storepass $(KEYSTORE_PASS) -signedjar vncs.jar vnc.jar $(KEYSTORE_ALIAS))
	@(mv vncs.jar vnc.jar)

deploy: sign
	@(mkdir -p examples/applet)
	@(cp vnc.jar examples/applet/)

runserver: deploy
	@(./examples/dev_appserver.py)
