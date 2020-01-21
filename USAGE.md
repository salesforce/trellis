# Using Trellis

[Plugin Configuration](#plugin-configuration)\
[Files](#files)\
[Groups](#groups)\
[Rules](#rules)\
[Whitelists](#whitelists)\
[Expressions](#expressions)\
[Properties](#properties)\
[Paths](#paths)

## Plugin Configuration

Trellis is intended for use in multi-module projects; it is most useful in the case where you need to enforce dependency policies across multiple modules.  You usually will want to configure it once in your root/parent pom.

## Plugin Configuration Example

```
<plugins>
  <plugin>
    <groupId>com.salesforce.trellis</groupId>
    <artifactId>trellis-maven-plugin</artifactId>
    <version>0.0.5</version>
    <configuration>
      <configFiles>
        <configFile>${maven.multiModuleProjectDirectory}/dependency-rules.yaml</configFile>
        <configFile>${env.specialRulesDir}/*.yaml</configFile>
      </configFiles>
      <properties>
        <trellis.whitelist.headerComment>GENERATED FILE - DO NOT EDIT</trellis.whitelist.headerComment>
      </properties>
    </configuration>
  </plugin>
</plugins>
```

Adding this configuration to the parent pom will cause the plugin to be executed on all of the child modules.  

Assuming you want the same rules applied in each case, some care must be taken to ensure that the rules are loaded from the same place.  The ``${project.basedir}`` variable is not appropriate for this as it will always be the basedir of the module being validated; that's why the example here uses ``${maven.multiModuleProjectDirectory}`` (which is new in maven 3.6).


### Plugin Configuration Parameters

| Parameter | Meaning |
| --------- | ------- |
| ``configFiles`` | Relative path to yaml file(s) containing rules.  May include simple wildcard (\*) expressions.  Multiple ``configFile`` elements may be specified in the ``configFiles``; at least one must be. |
| ``properties`` | Optional set of property values that will be made available for substitution in rules files alongside system and maven properties.  Same-named properties set here will take precedence.  There are some properties that have specific meaning to trellis, those are describe below. |

## Files

Rules are declared in one or more yaml files.  Each file has the following sections:

Section                          | Contents
-------                          | --------
[``groups``](#groups)            | Labelled artifact sets
[``rules``](#rules)              | The actual dependency constraint declarations
[``whitelists``](#whitelists)    | Detailed config for generated whitelists 
[``properties``](#properties)    | File-specific property settings

The sections may appear in the file in any order.
 
## Groups

Groups allow you to put a label on a set of maven artifacts.  This can be very convenient when you have a number of rules that need to operate on the same artifacts, or when the set definition is very complex.

#### Group Example

<pre>
<b>groups:</b>
- <b>name:</b> MY_IMPL_MODULES
  <b>includes:</b>
  - my-app:*-impl
  - my-other-app:*-impl

- <b>name:</b> BIG_MODULE_DEPENDENCIES
  <b>pomDependencies:</b>
  - ${maven.multiModuleProjectDirectory}/big-module/pom.xml
</pre>

#### Group Attributes

Each ``group`` element has the following attributes.

Attribute         | Required? | Meaning
---------         | --------- | ------------- 
`name`            | Yes       | The name by which the group can be referenced in rules and other groups.  The name must be a valid Java identifier (no spaces, no special characters) and does not support property interpolation.  For readability, we suggest that group names be in all-caps.
`includes`        | No        | A list of [expressions](#expressions) that identify that artifacts to be included in the group.
`pomDependencies` | No        | [Path](#paths) to another pom file, all of the `<dependencies>` of which will be included in the group.  This can be useful for restricting dependencies in aggregator projects.


## Rules

Groups allow you to put a label on a set of maven artifacts.  This can be very convenient when you have a number of rules that need to operate on the same artifacts, or when the set definition is very complex.

The `rules` section contains a list of rule elements that describe the dependency constraints to be enforced.

#### Rule Example

Here's a simple example of a rule that blocks non-test dependencies on junit and mockito.

<pre>
<b>rules:</b>
- <b>action:</b> DENY
- <b>from:</b>
  - myapp:*
  <b>to:</b>
  - junit:*
  - org.mockito:*
  <b>scope:</b>
  - compile, provided
  <b>reason:</b> Production code shouldn't depend on test frameworks.
</pre>

#### Rule Attributes

Each ``rule`` element has the following attributes.  Interpolated attributes support [property substitution](#properties).

Attribute  | Required? | Meaning
---------  | --------- | ------------- 
``action``     | Yes  |ALLOW, WARN, or DENY.  See [Rule Action](#rule-action) for details.
``from``       | Yes  |  One or more [expressions](#expressions) describing a set of artifacts.  The rule will be applied to all dependencies ``from`` any artifact in this set.
``to``       | Yes  |  One or more [expressions](#expressions) describing a set of artifacts.  The rule will be applied to all dependencies ``to`` any artifact in this set
``exceptFrom`` | No  |  One or more [expressions](#expressions) describing a set of artifacts.  The rule will be NOT be applied to any dependencies from artifacts in this set (even if they are present in the ``from`` set).
``exceptTo``       | No  | One or more [expressions](#expressions) describing a set of artifacts.  The rule will be NOT be applied to any dependencies to artifacts in this set (even if they are present in the ``to`` set).
``scope``    | No  | A list of maven scopes.  If specified, the rule will applied only to dependencies of those scopes.  By default, rules apply to dependencies of any scope.
``reason``   | No  | Explanatory text that will be included in the logs if `action` is WARN or DENY
``distance`` | No  | ANY, DIRECT_ONLY or TRANSIENT_ONLY.  See [Rule Distance](#rule-distance) for details.
``optionality`` | No  | ANY, OPTIONAL_ONLY or NON_OPTIONAL_ONLY.  See [Rule Optionality](#rule-optionality) for details.

``whitelist``| No  | [Path](#paths) to a file that describes exceptions to this rule.  See [Whitelists](#whitelists) for details. 

#### Rule Action

The ``action`` property of a rule is required and has the following valid values.  

Note that the actions here are ordered in descending order of precedence; `ALLOW` rules have precedence over `WARN`; `WARN` has precedence over `DENY`.

Value    | Meaning
------   | -------
``ALLOW``  | No action is taken.  The dependency is allowed and further evaluation of rules stops.  
``WARN``   | A warning will be output into the logs saying that the dependency is undesireable.  If 'reason' is set, that text will be included in the log message.
``DENY``   | The build will fail along with an explanatory error message in the logs.  If 'reason' is set, that text will be included in the log message.

#### Rule Distance

The `distance` attribute is used to limit the dependencies to which a Rule applies based on the distance between the 'from' and the 'to' nodes in the graph of declared dependencies.  It must be one of the following:

Value              | Meaning
------             | -------
``ANY``            | The rule applies to all dependencies.  This is the default.
``DIRECT_ONLY``    | The rule applies only to direct dependencies (i.e., distance == 1).
``TRANSIENT_ONLY`` | The rule applies only to transitive dependencies (i.e., distance > 1).


#### Rule Distance

The `optionality` attribute of a rule is used to limit the dependencies to which a Rule applies based on whether or not they are optional.  It must be one of the following:

Value              | Meaning
------             | -------
``ANY``            | The rule applies to all dependencies.  This is the default.
``OPTIONAL_ONLY``  | The rule applies only to optional dependencies.
``NON_OPTIONAL_ONLY`` | The rule applies only depedencies that are not optional.

## Whitelists

Rules may specify a `whitelist` file that specifies exceptions to the rule.  Whitelists help you 'stop the bleeding' while you work to pay down technical debt that blocks the immediate removal of undesirable dependencies.

Multiple rules declared in the same rules file may use the same whitelist file.

### Customizing Whitelist Files

The whitelist section contains optional customizations that apply to whitelist files.

### Whitelist Attributes

Each ``whitelist`` element has the following attributes.  Interpolated attributes support [property substitution](#properties).

Attribute       | Required? | Meaning
---------       | --------- | -------
`file`          | Required  | [Path](#paths) to the whitelist file.  This must match the ``whitelist`` specified in one of the rules in the rules file
`action`        | Optional  | Action that will be used in the generated rules.  The default is `WARN`.
`headerComment` | Optional  | Comment text that will be placed at the top of the generated whitelist file.

### Whitelist Declaration Example

Here's a simple example that shows how we could start to wean our codebase off of its dependency on ``bad:module``.

<pre>
- action: DENY
  from:
  - myapp:*
  to:
  - bad:module
  reason: We need to stop using bad:module.
  - <b>whitelist: debt-whitelist.yaml</b>

- whitelists:
  - <b>whitelist: debt-whitelist.yaml</b>
    headerComment: This file represents a technical debt in our codebase.  Let's pay it down!
</pre>

After [generating the whitelist file](#generating-whitelist-files), we would end up with a ``debt-whitelist.yaml`` file that looks something like this:

<pre>
#
# This file represents a technical debt in our codebase.  Let's pay it down!
#
- action: WARN
  from:
  - myapp:foo
  - myapp:bar
  - myapp:baz
  to:
  - bad:module
  reason: We need to stop using bad:module
</pre>

### Generating Whitelist Files

After configuring a whitelist on a rule (see below), you can automatically generate the whitelist file byrun the following maven command:

```mvn trellis:update-whitelists```

This will cause a new rule file to be generated that contains additional `WARN` rule declarations for all dependencies that are found in violation of the declared rule.  Then, during normal processing, the whitelist rules will be automatically imported and given precedence over the rule, effectively granting exceptions to the 'bad' dependencies that were in place when the update-whitelist goal was run.

If progress is made at a later time to remove the bad dependencies, running `update-whitelists` again will automatically remove rules for whatever exceptions are no longer needed.

## Expressions

Expressions are used to identify maven artifacts to which rules will be applied.  They are used to define both ``groups`` and ``rules``.

There are three kinds of expressions:

#### 1. Artifact Expression

A colon-separated groupId/artifactId pair that identifies a specific artifact:

```
myGroup:myArtifact
```

#### 2. Wildcard Expression

A colon-separated groupId/artifactId pair that does simple asterisk wildcard matching:

```
myproject-*:*-api
```

#### 3. Group Expression

The name of a group that was described earlier in the rules document.  Groups may refer to other groups but forward references are not allowed.

<pre>
groups:
- name: <b>SMALLER_GROUP</b>
  includes: myapp:myapp-impl

- name: BIGGER_GROUP
  inclues:
  - <b>SMALLER_GROUP</b>
  - other:stuff
</pre>

## Properties

Attributes in rule files are interpolated using standard maven property syntax.  Property values comes from the following sources, in ascending order of precedence:

* system properties
* maven properties
* [plugin configuration](#plugin-configuration-parameters) properties
* [file-level properties](#file-level-properties)

We recommend that you use properties judiciously; overuse can reduce the readability of your files.

#### File-level Properties

Each yaml file may include a ``properties`` element that declared arbitrary properties.  These properties be available for interpolation in the file in which they are declared.

#### Properties Example

<pre>
<b>properties:</b>
- <b>whitelistDir:</b> ${maven.multiModuleProjectDirectory}/misc/trellis/whitelists
- <b>badstuff.reason:</b> Just don't depend on badstuff, please.

rules:
- action: DENY
- from: [myapp:*]
  to: [badstuff:*]
  reason: <b>${badstuff.reason}</b>
  whitelist: <b>${whitelistDir}</b>/badstuff-whitelist.yaml
</pre>

#### Special Properties

There are a few properties you can use to set default behaviors for trellis. 

Property | Meaning
-------- | -------
``trellis.whitelist.headercomment`` | Comment that will appear in the header of all generated whitelists.  You probably want to configure this at the plugin level so that all of your generated whitelists will share a common header.
``trellis.whitelist.action`` | Default action for generated rules in the white list file  The default action is WARN but this property is useful if you instead would like to simply ALLOW rule violations that have been whitelisted.
``trellis.rule.reason`` | Default text that will be used as the ``reason`` for a rule.  You may want to configure this at the file level so that all rules share a common reason.


## Paths

Unless otherwise noted, configuration attributes that describe file paths are always resolved relative to the file which they are declared.

For example, given a rules file at this absolute path:

```
/home/me/myproject/my-rules.yaml
```

that contains a declaration like this:

<pre>
rules:
- action: DENY
  to:
  ...
  <b>whitelist: whitelists/my-whitelist.yaml</b>
</pre>

the whitelist file will be resolved at

```
/home/me/myproject/whitelists/my-whitelist.yaml
```
