# Building Pixelitor from the source code

Pixelitor requires Java 15+ to compile. 

## Starting Pixelitor in an IDE

When you start the program from an IDE, use **pixelitor.Pixelitor** as the main class.

## Building the Pixelitor jar file from the command line

1. Install [Maven](https://maven.apache.org/install.html)
2. Check the Maven installation with `mvn --version`
3. Execute `mvn clean package` in the main directory (where the pom.xml file is), this will create an executable jar in the `target` subdirectory. If you didn't change anything, or if you only changed translations/icons, then you can skip the tests by running `mvn clean package -Dmaven.test.skip=true` instead.  
