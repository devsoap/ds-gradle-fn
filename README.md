# Fn Gradle Plugin

The Fn Gradle plugin allows you to easily build functions with Java, Groovy or Kotlin for deploying to your Fn Project (https://fnproject.io) server.

The plugin provides helpers for creating a function, building the docker image and deploying it to the server. Under the 
hood the plugin uses the Fn CLI (https://github.com/fnproject/cli) to perform the actions.

# Getting started

The only requirement to use the Gradle plugin is to install **docker** on your local machine. Once you have that done
you can continue with the tutorial.

In a terminal create a new folder and in that folder create a *build.gradle* file with the following contents:
```
plugins {
  id 'groovy'
  id 'com.devsoap.fn' version '0.0.9'
}

version = '1.0.0'

repositories {
   mavenCentral()
}

dependencies {
   compile group: 'org.codehaus.groovy', name: 'groovy', version: '2.5.2'
}

fn {
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

Start your FN server with by running ``gradle fnStart`` to start the fn server locally.

Once the Fn server is running we can deploy our function there, in the project folder execute the following command to 
package our function in a docker image and deploy it:
```
gradle fnDeploy
```

If everything went well we can now invoke our function with
```
gradle fnInvoke
```
or if you want to provide some input to the function with
```
gradle fnInvoke --input=John
```

> **Development tip**: If you are developing run ``gradle -t fnInvoke`` instead. While that is running you can change 
the source code and Gradle will auto-deploy a new version after every change and invoke it so you can immediately see 
the function result after the change.

> **Development tip**: You can pass headers to the function by using ``--headers=X-foo=bar,X-Other=Baz``.

> **Development tip**: You can pass query parameters to the function by using ``--params=foo=bar,other=baz``
`

If you want to use a browser instead, you can access the function url at ``http://localhost:8080/t/<functionName>/<functionName>``.

# Sponsors

Support this project by becoming a sponsor. Your logo will show up here with a link to your website. To become a sponsor send a mail to sponsor@devsoap.com.

# License

This plugin is distributed under the Apache License 2.0 license. For more information about the license see the LICENSE file 
in the root directory of the repository. A signed CLA is required when contributing to the project.
