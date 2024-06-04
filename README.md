### Adding Rabinizer 3.1 to local libs

This project uses the external [Rabinizer 3 Library](https://www7.in.tum.de/~kretinsk/rabinizer3.html) extensively to
deal with the translation process from string-based LTL predicates to Deterministic Rabin Automata. Since this is not
readily available on a public Repository, we will need to manually add it to our project. To do so:

+ Download the ```rabinizer3.1.jar``` file
  from [their official source](https://www7.in.tum.de/~kretinsk/rabinizer3/rabinizer3.1.jar).
+ Add the downloaded ```.jar``` file to the folder ```predicate-compiler/libs/```.
  The [```build.gradle.ts```](build.gradle.kts) file includes a local files implementation pointing to this route.