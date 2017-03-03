Interactive exercises in a gitbook
==============

With this plugin, a book can contain interactive exercises (currently only in Javascript). An exercise is a code challenge provided to the reader, who is given a code editor to write a solution which is checked against the book author's validation code.

## How to use it?

To use the exercises plugin in your Gitbook project, add the `exercises` plugin to the `book.json` file, then install plugins using `gitbook install`.

```
{
    "plugins": ["exercises"]
}
```

## Exercises format

An exercise is defined by 4 simple parts:

* Exercise **Message**/Goals (in markdown/text)
* **Initial** code to show to the user, providing a starting point
* **Solution** code, being a correct solution to the exercise
* **Validation** code that tests the correctness of the user's input
* **Context** (optional) code evaluated before executing the user's solution

```
{% exercise %}
Define a variable `x` equal to 10.

{% initial %}
var x =

{% solution %}
var x = 10;

{% validation %}
assert(x == 10);

{% context %}
// This is context code available everywhere
// The user will be able to evaluate `exposedVar`
var exposedVar = 3;
// ... or call `exposedFunction`
function exposedFunction {
    return 3;
}
{% endexercise %}
```

**The old format (`gitbook < 2.0.0`) is no longer supported.**
