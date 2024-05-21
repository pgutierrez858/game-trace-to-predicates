### Installing Rabinizer 3.1 to local Maven Repository

This project uses the external [Rabinizer 3 Library](https://www7.in.tum.de/~kretinsk/rabinizer3.html) extensively to deal with the translation process from string-based LTL predicates to Deterministic Rabin Automata. Since this is not readily available on a public Maven Repository, manual installation is required. To do so:

+ Download the ```rabinizer3.1.jar``` file from [their official source](https://www7.in.tum.de/~kretinsk/rabinizer3/rabinizer3.1.jar).
+ Ensure you have Maven installed in your system and then run ```mvn install:install-file -Dfile="rabinizer3.1.jar" -DgroupId="com.rabinizer" -DartifactId=rabinizer3 -Dversion="3.1" -Dpackaging=jar -DgeneratePom=true``` from the path where you downloaded the JAR file from the first step. This installs the .jar dependency as a local Maven repository. 
+ In the project's ```pom.xml``` file, there is a predefined reference to what will now default to the local Maven repository you just installed:
```
<dependency>
    <groupId>com.rabinizer</groupId>
    <artifactId>rabinizer3</artifactId>
    <version>3.1</version>
</dependency>
```