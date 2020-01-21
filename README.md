
# trellis
<img align="right" src="README.jpg"  height="150">
<p>
Trellis is a Maven plugin that helps manage and prevent dependency entanglement in large projects.
</p>
<p>
It allows you to enforce simple, high-level rules that constrain how modules are allowed to depend on one another.  Code changes that violate your rules will cause a build failure.
</p>

## Features

* wildcard matching on group/artifact ids
* composable group definitions
* automatic whitelisting


## Examples

Rules are declared in simple yaml files that live in the root of your maven project.  Here some examples of the kinds of rules you can declare:

**Ensure that only a specific module is allowed to depend on my 'secret-api' module:**

```
rules:
- action: ALLOW
  from: my:chosen-module
  to: my:secret-api

- action: DENY
  from: my:*
  to: my:secret-api
```

**Enforce a naming convention in which 'api' modules are not allowed to depend on 'impl' modules:**

```
groups:
- name: API_MODULES
  includes:
  - *:*-api

- name: IMPL_MODULES
  includes:
  - *:*-impl

rules:
- action: DENY
  from: [API_MODULES]
  to: [IMPL_MODULES]
  reason: apis shouldn't depend on impls.
```

**Ensure that your production code doesn't rely on testing frameworks:**

```
groups:
- name: PRODUCTION_CODE
  includes:
  - my.app:*

- name: TEST_FRAMEWORKS
  includes:
  - junit:*

rules:
- action: DENY
  from: [PRODUCTION_CODE]
  exceptFrom: [my.app:special-case]
  to: [TEST_FRAMEWORKS]
  reason: prod code should be using test frameworks
  
```

## Usage

Detailed usage documentation is available in [here](USAGE.md).

