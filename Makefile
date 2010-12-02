OUTPUT = bin
SOURCES = $(shell find src -type f -name \*.java)
FLAGS = -target 1.5 -d $(OUTPUT)
KEYSTORE_ALIAS = "steam"

all: sign

build:
	@(javac $(FLAGS) $(SOURCES))

jar: build
	@(jar cmf manifest.mf vnc.jar -C bin/ .)

sign: jar
	@(jarsigner -signedjar vncs.jar vnc.jar $(KEYSTORE_ALIAS))
	@(mv vncs.jar vnc.jar)

deploy: sign
	@(mkdir -p examples/applet)
	@(cp vnc.jar examples/applet/)

