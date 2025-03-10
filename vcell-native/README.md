1. Create native executable of module
   1. Create clean instance
   2. Record runtime dependent operations for future replay
   3. Build the native executable 
```bash
pushd ../vcell
mvn clean install -DskipTests
popd
```
export JAVA_HOME=$(jenv javahome)
```bash
mvn clean install
java -agentlib:native-image-agent=config-output-dir=target/recording \
-jar target/vcell-native-1.0-SNAPSHOT.jar \
"$(pwd)/src/test/resources/TinySpacialProject_Application0.xml" \
"$(pwd)/target/sbml-input"

mvn package -P shared-dll -DskipTests=true
```

Sources: 
- https://www.graalvm.org/jdk21/reference-manual/native-image/dynamic-features/Resources/
- https://www.graalvm.org/latest/reference-manual/native-image/metadata/AutomaticMetadataCollection/

2. Run native executable
```bash
pushd ..
./target/sbml_to* -Dheadless=true \
"./vcell/vcell-rest/src/test/resources/TinySpacialProject_Application0.xml" \
".vcell/vcell-nativelib/target/sbml-input" 
popd
```