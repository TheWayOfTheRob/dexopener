# DexOpener

[![CircleCI](https://circleci.com/gh/tmurakami/dexopener.svg?style=shield)](https://circleci.com/gh/tmurakami/dexopener)
[![Release](https://jitpack.io/v/tmurakami/dexopener.svg)](https://jitpack.io/#tmurakami/dexopener)
[![Javadoc](https://img.shields.io/badge/Javadoc-2.0.0-alpha01-brightgreen.svg)](https://jitpack.io/com/github/tmurakami/dexopener/2.0.0-alpha01/javadoc/)<br>
![Android](https://img.shields.io/badge/Android-4.1%2B-blue.svg)

A library that provides the ability to mock your final classes on
Android.

## Installation

```groovy
repositories {
    google()
    maven { url 'https://jitpack.io' }
}

dependencies {
    androidTestImplementation 'com.github.tmurakami:dexopener:2.0.0-alpha01'

    androidTestImplementation 'androidx.test:runner:x.y.z
    // DexOpener is also usable together with the support test runner.
    // androidTestImplementation 'com.android.support.test:runner:x.y.z'
}
```

## Usage

Add an AndroidJUnitRunner subclass into your app's **androidTest**
directory.

```java
// Specify your root package as `package` statement.
// The final classes you can mock are only in the package and its subpackages.
package your.root.pkg;

public class YourAndroidJUnitRunner extends AndroidJUnitRunner {
    @Override
    public Application newApplication(ClassLoader cl, String className, Context context)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        DexOpener.install(this); // Call me first!
        return super.newApplication(cl, className, context);
    }
}
```

Then specify your AndroidJUnitRunner as the default test instrumentation
runner in your app's build.gradle.

```groovy
android {
    defaultConfig {
        minSdkVersion 16 // 16 or higher
        testInstrumentationRunner 'your.root.pkg.YourAndroidJUnitRunner'
    }
}
```

You can see some examples in the [dexopener-example](dexopener-example) directory.

## Tips

### Replacing the Application instance for testing

To instantiate your custom `android.app.Application` object other than
default application, pass a string literal of that class name as the
second argument to `super.newApplication()` in your test instrumentation
runner.

```java
@Override
public Application newApplication(ClassLoader cl, String className, Context context)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    DexOpener.install(this); // Call me first!
    return super.newApplication(cl, "your.app.YourTestApplication", context);
}
```

**Do not call `Class#getName()` to get your Application class name**.
The following code may cause a class inconsistency error.

```java
// This code may cause a class inconsistency error.
return super.newApplication(cl, YourTestApplication.class.getName(), context);
````

## Alternatives

### [Kotlin all-open compiler plugin](https://kotlinlang.org/docs/reference/compiler-plugins.html#all-open-compiler-plugin)

DexOpener removes the final modifier from your final classes and creates
dex files to make the application class loader load the classes.
However, they are not so lightweight. If you want to save even a little
testing time of your Kotlin app, you can introduce [the all-open compiler plugin](https://kotlinlang.org/docs/reference/compiler-plugins.html#all-open-compiler-plugin)
instead of DexOpener.

[This comment](https://github.com/mockito/mockito/issues/1082#issuecomment-301646307)
will help you to open your classes only for testing. You can also find
out how to use the `OpenForTesting` annotation in [Google's samples for Android Architecture Components](https://github.com/googlesamples/android-architecture-components).

### [DexMaker-Mockito inline mocking](https://github.com/linkedin/dexmaker)

You can now even stub the final methods of the Android API using the
`dexmaker-mockito-inline` library. In addition, the
`dexmaker-mockito-inline-extended` library supports for stubbing static
methods and spying on an object created by the Android system such as
Activity. [Here](https://medium.com/androiddevelopers/mock-final-and-static-methods-on-android-devices-b383da1363ad)
is an introduction article.

Note that these libraries will only work on Android 9 Pie (API 28) or
higher devices.

## Notice

This library contains the classes of the following libraries:

- [ClassInjector](https://github.com/tmurakami/classinjector)
- [dexlib2 (part of smali/baksmali)](https://github.com/JesusFreke/smali)
- [Guava (on which dexlib2 relies)](https://github.com/google/guava)

They have been minified with
[ProGuard](https://www.guardsquare.com/en/proguard) and repackaged with
[Jar Jar Links](https://github.com/pantsbuild/jarjar). In addition,
dexlib2 has been backported to Java 7 with
[Retrolambda](https://github.com/orfjackal/retrolambda).

## License

```
Copyright 2016 Tsuyoshi Murakami

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
