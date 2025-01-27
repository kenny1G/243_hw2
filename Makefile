SHELL = /bin/bash

# compilation related variables -- please don't modify!
JAVA_CLASSES = $(shell find src -name "*.java")
JC = $(JAVA_HOME)/bin/javac
CP = lib/joeq.jar
JFLAGS = -Xlint:-options -source 1.5 -target 1.5 -cp $(CP)
BUILD_DIR = build/

# modify TESTS to change the set of test files
TEST_DIR = src/test/
# TESTS = $(patsubst $(TEST_DIR)%.java, %, $(wildcard $(TEST_DIR)*.java)) # automatically collects all test files under src/test/
TESTS = Test1 Test2

# Late definitions override the earlier ones, so just keep the latest as your target
# you can also use `FLOW=xxx make test` in command line as an alternate to toggling this variable in the file
ifndef FLOW
FLOW = ConstantProp
# FLOW = Liveness
# FLOW = ReachingDefs
endif

# `make compile` builds Java bytecode from source code and put them into build/
compile: clean
	@mkdir -p $(BUILD_DIR)
	@$(JC) $(JFLAGS) -d $(BUILD_DIR) $(JAVA_CLASSES)
	@chmod +x run.sh

%.check: compile
	@java -cp $(BUILD_DIR):$(CP) flow.Flow flow.MySolver flow.$(FLOW) test.$*

# `make <test_name>.test` runs `diff` on the dataflow output from your program and the reference output for <test_name>
%.test: compile
	@echo "Running test: $* with $(FLOW) as flow!"
	@java -cp $(BUILD_DIR):$(CP) flow.Flow flow.MySolver flow.$(FLOW) test.$* | \
	diff $(TEST_DIR)$*.$(FLOW).out -
	@echo "The solver passes test: $* with $(FLOW) as flow!"

# `make test` runs check on all test cases
test: $(patsubst %, %.test, $(TESTS))

clean:
	@rm -rf $(BUILD_DIR)

# after you are done, run `make submission` and submit `submission.zip` to Gradescope
# there is no need to submit your customized test files!
submission: id.txt src/flow/MySolver.java src/flow/Faintness.java src/flow/ReachingDefs.java
	zip -r $@ $^
