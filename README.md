# Fn Gradle Plugin

The Fn Gradle plugin allows you to easily build functions with Java, Groovy or Kotlin for deploying to your Fn Project (https://fnproject.io) server.

The plugin provides helpers for creating a function, building the docker image and deploying it to the server. Under the 
hook the plugin uses the Fn CLI (https://github.com/fnproject/cli) to perform the actions.

# Getting started

The first thing you need is install **docker** on your local machine. 

Next, you will need to install the Fn Project CLI, see https://github.com/fnproject/cli for instructions.

Once that is done you are all set up to create your first function.

In a terminal create a new folder and in that folder create a *build.gradle* file with the following contents:
```
plugins {
  id 'groovy'
  id 'com.devsoap.fn' version '0.0.1'
}

version = '1.0.0'

repositories {
   mavenCentral()
}

dependencies {
   compile group: 'org.codehaus.groovy', name: 'groovy-all', version: '2.5.2'
}

fnDocker {
  functionClass = 'com.example.fn.MyFunction'
  functionMethod = 'handleRequest'
}

```

Next run the following command in your terminal to create the project:
```
gradle fnCreateFunction
```

You should now have the following project structure:
```
.
├── build.gradle
└── src
    └── main
        └── groovy
            └── com
                └── example
                    └── fn
                        └── MyFunction.groovy

6 directories, 2 files
```

Start your FN server with by running ``fn start`` to start the fn server locally.

Once the Fn server is running we can deploy our function there, in the project folder execute the following command to 
package our function in a docker image and deploy it:
```
gradle fnDeploy
```

If everything went well we can now invoke our function with
```
gradle fnInvoke
```

> **Development tip**: If you are developing run ``gradle -t fnInvoke`` instead. While that is running you can change 
the source code and Gradle will auto-deploy a new version after every change and invoke it so you can immediately see 
the function result after the change.

If you want to use a browser instead, you can access the function url at ``http://localhost:8080/t/<functionName>``.


# License

This plugin is distributed under the Apache License 2.0 license. For more information about the license see the LICENSE file 
in the root directory of the repository. A signed CLA is required when contributing to the project.
