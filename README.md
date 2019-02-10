# Maven Javassist Plugin

## Add the following to your pom.xml

Each group of students should choose one of the assignments listed below. All assignments involve a form of **test execution**, 
**dynamic analysis**, **program inspection** and **code instrumentation**.

```XML:
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.0.0-M3</version>
        <configuration>
            <classesDirectory>${maven-javassist-directory}</classesDirectory>
        </configuration>
    </plugin>
    <plugin>
        <groupId>fr.istic.vv.maven.javassist</groupId>
        <artifactId>javassist-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <configuration>
            <packageName>pro.vinyard</packageName>
            <outputDirectory>${maven-javassist-directory}</outputDirectory>
        </configuration>
        <executions>
            <execution>
                <phase>process-classes</phase>
                <goals>
                    <goal>javassist</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
</plugins>
 ```
 
 ```XML:
 <properties>
    <maven-javassist-directory>${project.build.outputDirectory}/jassist</maven-javassist-directory>
 </properties>
 ```
 
 ## Run the code coverage analysis
 
 To run the code coverage report you just have to run the maven goal **package**.
 You will find a file named **jassist.csv** in your folder **target/classes/jassist/**.
 
 In these file you will find for each block of code a CSV data that indicates if class file, the line of the begin of the block, and true or false if the block have been covered or not.
 
 ## Example in a test project that implement maven javassist plugin
 
 You will find an example of a project that implements the code coverage plugin at this github repository : https://github.com/Viinyard/Maven_javassist_plugin_TEST.git