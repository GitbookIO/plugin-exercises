/*
 * Copyright 2008 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.javascript.jscomp;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.jstype.JSTypeNative;
import com.google.javascript.rhino.jstype.JSTypeRegistry;
import com.google.javascript.rhino.testing.BaseJSTypeTestCase;
import com.google.javascript.rhino.testing.TestErrorReporter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Unit test for the Compiler DisambiguateProprties pass.
 *
 */
public class DisambiguatePropertiesTest extends CompilerTestCase {
  private DisambiguateProperties<?> lastPass;
  private boolean runTightenTypes;

  public DisambiguatePropertiesTest() {
    parseTypeInfo = true;
  }

  @Override
  public CompilerPass getProcessor(final Compiler compiler) {
    final TypeCheck checker = new TypeCheck(compiler,
        new SemanticReverseAbstractInterpreter(
            compiler.getCodingConvention(), compiler.getTypeRegistry()),
        compiler.getTypeRegistry());

    return new CompilerPass() {
      @Override
      public void process(Node externs, Node root) {
        checker.processForTesting(externs, root);

        Map<String, CheckLevel> propertiesToErrorFor =
            Maps.<String, CheckLevel>newHashMap();
        propertiesToErrorFor.put("foobar", CheckLevel.ERROR);

        if (runTightenTypes) {
          TightenTypes tightener = new TightenTypes(compiler);
          tightener.process(externs, root);
          lastPass = DisambiguateProperties.forConcreteTypeSystem(compiler,
              tightener, propertiesToErrorFor);
        } else {
          // This must be created after type checking is run as it depends on
          // any mismatches found during checking.
          lastPass = DisambiguateProperties.forJSTypeSystem(
              compiler, propertiesToErrorFor);
        }

        lastPass.process(externs, root);
      }
    };
  }

  @Override
  protected int getNumRepetitions() {
    return 1;
  }

  public void testOneType1() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.a = 0;\n"
        + "/** @type Foo */\n"
        + "var F = new Foo;\n"
        + "F.a = 0;";
    testSets(false, js, js, "{a=[[Foo.prototype]]}");
    testSets(true, js, js, "{a=[[Foo.prototype]]}");
  }

  public void testOneType2() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype = {a: 0};\n"
        + "/** @type Foo */\n"
        + "var F = new Foo;\n"
        + "F.a = 0;";
    String expected = "{a=[[Foo.prototype]]}";
    testSets(false, js, js, expected);
    testSets(true, js, js, expected);
  }

  public void testOneType3() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype = { get a() {return  0},"
        + "                  set a(b) {} };\n"
        + "/** @type Foo */\n"
        + "var F = new Foo;\n"
        + "F.a = 0;";
    String expected = "{a=[[Foo.prototype]]}";
    testSets(false, js, js, expected);
    testSets(true, js, js, expected);
  }

  public void testPrototypeAndInstance() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.a = 0;\n"
        + "/** @type Foo */\n"
        + "var F = new Foo;\n"
        + "F.a = 0;";
    testSets(false, js, js, "{a=[[Foo.prototype]]}");
    testSets(true, js, js, "{a=[[Foo.prototype]]}");
  }

  public void testTwoTypes1() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.a = 0;"
        + "/** @type Foo */\n"
        + "var F = new Foo;\n"
        + "F.a = 0;"
        + "/** @constructor */ function Bar() {}\n"
        + "Bar.prototype.a = 0;"
        + "/** @type Bar */\n"
        + "var B = new Bar;\n"
        + "B.a = 0;";
    String output = ""
        + "function Foo(){}"
        + "Foo.prototype.Foo_prototype$a=0;"
        + "var F=new Foo;"
        + "F.Foo_prototype$a=0;"
        + "function Bar(){}"
        + "Bar.prototype.Bar_prototype$a=0;"
        + "var B=new Bar;"
        + "B.Bar_prototype$a=0";
    testSets(false, js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
    testSets(true, js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
  }

  public void testTwoTypes2() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype = {a: 0};"
        + "/** @type Foo */\n"
        + "var F = new Foo;\n"
        + "F.a = 0;"
        + "/** @constructor */ function Bar() {}\n"
        + "Bar.prototype = {a: 0};"
        + "/** @type Bar */\n"
        + "var B = new Bar;\n"
        + "B.a = 0;";

    String output = ""
        + "function Foo(){}"
        + "Foo.prototype = {Foo_prototype$a: 0};"
        + "var F=new Foo;"
        + "F.Foo_prototype$a=0;"
        + "function Bar(){}"
        + "Bar.prototype = {Bar_prototype$a: 0};"
        + "var B=new Bar;"
        + "B.Bar_prototype$a=0";

    testSets(false, js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
    testSets(true, js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
  }

  public void testTwoTypes3() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype = { get a() {return  0},"
        + "                  set a(b) {} };\n"
        + "/** @type Foo */\n"
        + "var F = new Foo;\n"
        + "F.a = 0;"
        + "/** @constructor */ function Bar() {}\n"
        + "Bar.prototype = { get a() {return  0},"
        + "                  set a(b) {} };\n"
        + "/** @type Bar */\n"
        + "var B = new Bar;\n"
        + "B.a = 0;";

    String output = ""
        + "function Foo(){}"
        + "Foo.prototype = { get Foo_prototype$a() {return  0},"
        + "                  set Foo_prototype$a(b) {} };\n"
        + "var F=new Foo;"
        + "F.Foo_prototype$a=0;"
        + "function Bar(){}"
        + "Bar.prototype = { get Bar_prototype$a() {return  0},"
        + "                  set Bar_prototype$a(b) {} };\n"
        + "var B=new Bar;"
        + "B.Bar_prototype$a=0";

    testSets(false, js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
    testSets(true, js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
  }

  public void testTwoFields() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.a = 0;"
        + "Foo.prototype.b = 0;"
        + "/** @type Foo */\n"
        + "var F = new Foo;\n"
        + "F.a = 0;"
        + "F.b = 0;";
    String output = "function Foo(){}Foo.prototype.a=0;Foo.prototype.b=0;"
        + "var F=new Foo;F.a=0;F.b=0";
    testSets(false, js, output, "{a=[[Foo.prototype]], b=[[Foo.prototype]]}");
    testSets(true, js, output, "{a=[[Foo.prototype]], b=[[Foo.prototype]]}");
  }

  public void testTwoSeparateFieldsTwoTypes() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.a = 0;"
        + "Foo.prototype.b = 0;"
        + "/** @type Foo */\n"
        + "var F = new Foo;\n"
        + "F.a = 0;"
        + "F.b = 0;"
        + "/** @constructor */ function Bar() {}\n"
        + "Bar.prototype.a = 0;"
        + "Bar.prototype.b = 0;"
        + "/** @type Bar */\n"
        + "var B = new Bar;\n"
        + "B.a = 0;"
        + "B.b = 0;";
    String output = ""
        + "function Foo(){}"
        + "Foo.prototype.Foo_prototype$a=0;"
        + "Foo.prototype.Foo_prototype$b=0;"
        + "var F=new Foo;"
        + "F.Foo_prototype$a=0;"
        + "F.Foo_prototype$b=0;"
        + "function Bar(){}"
        + "Bar.prototype.Bar_prototype$a=0;"
        + "Bar.prototype.Bar_prototype$b=0;"
        + "var B=new Bar;"
        + "B.Bar_prototype$a=0;"
        + "B.Bar_prototype$b=0";
    testSets(false, js, output, "{a=[[Bar.prototype], [Foo.prototype]],"
                                + " b=[[Bar.prototype], [Foo.prototype]]}");
    testSets(true, js, output, "{a=[[Bar.prototype], [Foo.prototype]],"
                               + " b=[[Bar.prototype], [Foo.prototype]]}");
  }

  public void testUnionType() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.a = 0;"
        + "/** @constructor */ function Bar() {}\n"
        + "Bar.prototype.a = 0;"
        + "/** @type {Bar|Foo} */\n"
        + "var B = new Bar;\n"
        + "B.a = 0;\n"
        + "B = new Foo;\n"
        + "B.a = 0;\n"
        + "/** @constructor */ function Baz() {}\n"
        + "Baz.prototype.a = 0;\n";
    testSets(false, js,
             "{a=[[Bar.prototype, Foo.prototype], [Baz.prototype]]}");
    testSets(true, js, "{a=[[Bar.prototype, Foo.prototype], [Baz.prototype]]}");
  }

  public void testIgnoreUnknownType() {
    String js = ""
        + "/** @constructor */\n"
        + "function Foo() {}\n"
        + "Foo.prototype.blah = 3;\n"
        + "/** @type {Foo} */\n"
        + "var F = new Foo;\n"
        + "F.blah = 0;\n"
        + "var U = function() { return {} };\n"
        + "U().blah();";
    String expected = ""
        + "function Foo(){}Foo.prototype.blah=3;var F = new Foo;F.blah=0;"
        + "var U=function(){return{}};U().blah()";
    testSets(false, js, expected, "{}");
    testSets(true, BaseJSTypeTestCase.ALL_NATIVE_EXTERN_TYPES,
        js, expected, "{}");
  }

  public void testUnionTypeTwoFields() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.a = 0;\n"
        + "Foo.prototype.b = 0;\n"
        + "/** @constructor */ function Bar() {}\n"
        + "Bar.prototype.a = 0;\n"
        + "Bar.prototype.b = 0;\n"
        + "/** @type {Foo|Bar} */\n"
        + "var B = new Bar;\n"
        + "B.a = 0;\n"
        + "B.b = 0;\n"
        + "B = new Foo;\n"
        + "/** @constructor */ function Baz() {}\n"
        + "Baz.prototype.a = 0;\n"
        + "Baz.prototype.b = 0;\n";
    String output = ""
        + "function Foo(){}"
        + "Foo.prototype.Bar_prototype$a=0;"
        + "Foo.prototype.Bar_prototype$b=0;"
        + "function Bar(){}"
        + "Bar.prototype.Bar_prototype$a=0;"
        + "Bar.prototype.Bar_prototype$b=0;"
        + "var B=new Bar;"
        + "B.Bar_prototype$a=0;"
        + "B.Bar_prototype$b=0;"
        + "function Baz(){}"
        + "Baz.prototype.a$Baz_prototype=0;"
        + "Baz.prototype.b$Baz_prototype=0;";
    testSets(false, js, "{a=[[Bar.prototype, Foo.prototype], [Baz.prototype]],"
                 + " b=[[Bar.prototype, Foo.prototype], [Baz.prototype]]}");
    testSets(true, js, "{a=[[Bar.prototype, Foo.prototype], [Baz.prototype]],"
                 + " b=[[Bar.prototype, Foo.prototype], [Baz.prototype]]}");
  }

  public void testCast() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.a = 0;"
        + "/** @constructor */ function Bar() {}\n"
        + "Bar.prototype.a = 0;"
        + "/** @type {Foo|Bar} */\n"
        + "var F = new Foo;\n"
        + "(/** @type {Bar} */(F)).a = 0;";
    String output = ""
        + "function Foo(){}Foo.prototype.Foo_prototype$a=0;"
        + "function Bar(){}Bar.prototype.Bar_prototype$a=0;"
        + "var F=new Foo;F.Bar_prototype$a=0;";
    String ttOutput = ""
        + "function Foo(){}Foo.prototype.Foo_prototype$a=0;"
        + "function Bar(){}Bar.prototype.Bar_prototype$a=0;"
        + "var F=new Foo;F.Unique$1$a=0;";
    testSets(false, js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
    testSets(true, js, ttOutput,
        "{a=[[Bar.prototype], [Foo.prototype], [Unique$1]]}");
  }

  public void testConstructorFields() {
    String js = ""
      + "/** @constructor */\n"
      + "var Foo = function() { this.a = 0; };\n"
      + "/** @constructor */ function Bar() {}\n"
      + "Bar.prototype.a = 0;"
      + "new Foo";
    String output = ""
        + "var Foo=function(){this.Foo$a=0};"
        + "function Bar(){}"
        + "Bar.prototype.Bar_prototype$a=0;"
        + "new Foo";
    String ttOutput = ""
        + "var Foo=function(){this.Foo_prototype$a=0};"
        + "function Bar(){}"
        + "Bar.prototype.Bar_prototype$a=0;"
        + "new Foo";
    testSets(false, js, output, "{a=[[Bar.prototype], [Foo]]}");
    testSets(true, js, ttOutput, "{a=[[Bar.prototype], [Foo.prototype]]}");
  }

  public void testStaticProperty() {
    String js = ""
      + "/** @constructor */ function Foo() {} \n"
      + "/** @constructor */ function Bar() {}\n"
      + "Foo.a = 0;"
      + "Bar.a = 0;";
    String output = ""
        + "function Foo(){}"
        + "function Bar(){}"
        + "Foo.function__new_Foo___undefined$a = 0;"
        + "Bar.function__new_Bar___undefined$a = 0;";

    testSets(false, js, output,
        "{a=[[function (new:Bar): undefined]," +
        " [function (new:Foo): undefined]]}");
  }

  public void testSupertypeWithSameField() {
    String js = ""
      + "/** @constructor */ function Foo() {}\n"
      + "Foo.prototype.a = 0;\n"
      + "/** @constructor\n* @extends Foo */ function Bar() {}\n"
      + "/** @override */\n"
      + "Bar.prototype.a = 0;\n"
      + "/** @type Bar */ var B = new Bar;\n"
      + "B.a = 0;"
      + "/** @constructor */ function Baz() {}\n"
      + "Baz.prototype.a = function(){};\n";

    String output = ""
        + "function Foo(){}Foo.prototype.Foo_prototype$a=0;"
        + "function Bar(){}Bar.prototype.Foo_prototype$a=0;"
        + "var B = new Bar;B.Foo_prototype$a=0;"
        + "function Baz(){}Baz.prototype.Baz_prototype$a=function(){};";
    String ttOutput = ""
        + "function Foo(){}Foo.prototype.Foo_prototype$a=0;"
        + "function Bar(){}Bar.prototype.Bar_prototype$a=0;"
        + "var B = new Bar;B.Bar_prototype$a=0;"
        + "function Baz(){}Baz.prototype.Baz_prototype$a=function(){};";
    testSets(false, js, output, "{a=[[Baz.prototype], [Foo.prototype]]}");
    testSets(true, js, ttOutput,
        "{a=[[Bar.prototype], [Baz.prototype], [Foo.prototype]]}");
  }

  public void testScopedType() {
    String js = ""
        + "var g = {};\n"
        + "/** @constructor */ g.Foo = function() {}\n"
        + "g.Foo.prototype.a = 0;"
        + "/** @constructor */ g.Bar = function() {}\n"
        + "g.Bar.prototype.a = 0;";
    String output = ""
        + "var g={};"
        + "g.Foo=function(){};"
        + "g.Foo.prototype.g_Foo_prototype$a=0;"
        + "g.Bar=function(){};"
        + "g.Bar.prototype.g_Bar_prototype$a=0;";
    testSets(false, js, output, "{a=[[g.Bar.prototype], [g.Foo.prototype]]}");
    testSets(true, js, output, "{a=[[g.Bar.prototype], [g.Foo.prototype]]}");
  }

  public void testUnresolvedType() {
    // NOTE(nicksantos): This behavior seems very wrong to me.
    String js = ""
        + "var g = {};"
        + "/** @constructor \n @extends {?} */ "
        + "var Foo = function() {};\n"
        + "Foo.prototype.a = 0;"
        + "/** @constructor */ var Bar = function() {};\n"
        + "Bar.prototype.a = 0;";
    String output = ""
        + "var g={};"
        + "var Foo=function(){};"
        + "Foo.prototype.Foo_prototype$a=0;"
        + "var Bar=function(){};"
        + "Bar.prototype.Bar_prototype$a=0;";
    testSets(false, BaseJSTypeTestCase.ALL_NATIVE_EXTERN_TYPES,
        js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
    testSets(true, BaseJSTypeTestCase.ALL_NATIVE_EXTERN_TYPES,
        js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
  }

  public void testNamedType() {
    String js = ""
        + "var g = {};"
        + "/** @constructor \n @extends g.Late */ var Foo = function() {}\n"
        + "Foo.prototype.a = 0;"
        + "/** @constructor */ var Bar = function() {}\n"
        + "Bar.prototype.a = 0;"
        + "/** @constructor */ g.Late = function() {}";
    String output = ""
        + "var g={};"
        + "var Foo=function(){};"
        + "Foo.prototype.Foo_prototype$a=0;"
        + "var Bar=function(){};"
        + "Bar.prototype.Bar_prototype$a=0;"
        + "g.Late = function(){}";
    testSets(false, js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
    testSets(true, js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
  }

  public void testUnknownType() {
    String js = ""
        + "/** @constructor */ var Foo = function() {};\n"
        + "/** @constructor */ var Bar = function() {};\n"
        + "/** @return {?} */ function fun() {}\n"
        + "Foo.prototype.a = fun();\n"
        + "fun().a;\n"
        + "Bar.prototype.a = 0;";
    String ttOutput = ""
        + "var Foo=function(){};\n"
        + "var Bar=function(){};\n"
        + "function fun(){}\n"
        + "Foo.prototype.Foo_prototype$a=fun();\n"
        + "fun().Unique$1$a;\n"
        + "Bar.prototype.Bar_prototype$a=0;";
    testSets(false, js, js, "{}");
    testSets(true, BaseJSTypeTestCase.ALL_NATIVE_EXTERN_TYPES, js, ttOutput,
             "{a=[[Bar.prototype], [Foo.prototype], [Unique$1]]}");
  }

  public void testEnum() {
    String js = ""
        + "/** @enum {string} */ var En = {\n"
        + "  A: 'first',\n"
        + "  B: 'second'\n"
        + "};\n"
        + "var EA = En.A;\n"
        + "var EB = En.B;\n"
        + "/** @constructor */ function Foo(){};\n"
        + "Foo.prototype.A = 0;\n"
        + "Foo.prototype.B = 0;\n";
    String output = ""
        + "var En={A:'first',B:'second'};"
        + "var EA=En.A;"
        + "var EB=En.B;"
        + "function Foo(){};"
        + "Foo.prototype.Foo_prototype$A=0;"
        + "Foo.prototype.Foo_prototype$B=0";
    String ttOutput = ""
        + "var En={A:'first',B:'second'};"
        + "var EA=En.A;"
        + "var EB=En.B;"
        + "function Foo(){};"
        + "Foo.prototype.Foo_prototype$A=0;"
        + "Foo.prototype.Foo_prototype$B=0";
    testSets(false, js, output, "{A=[[Foo.prototype]], B=[[Foo.prototype]]}");
    testSets(true, js, ttOutput, "{A=[[Foo.prototype]], B=[[Foo.prototype]]}");
  }

  public void testEnumOfObjects() {
    String js = ""
        + "/** @constructor */ function Formatter() {}"
        + "Formatter.prototype.format = function() {};"
        + "/** @constructor */ function Unrelated() {}"
        + "Unrelated.prototype.format = function() {};"
        + "/** @enum {!Formatter} */ var Enum = {\n"
        + "  A: new Formatter()\n"
        + "};\n"
        + "Enum.A.format();\n";
    String output = ""
        + "/** @constructor */ function Formatter() {}"
        + "Formatter.prototype.Formatter_prototype$format = function() {};"
        + "/** @constructor */ function Unrelated() {}"
        + "Unrelated.prototype.Unrelated_prototype$format = function() {};"
        + "/** @enum {!Formatter} */ var Enum = {\n"
        + "  A: new Formatter()\n"
        + "};\n"
        + "Enum.A.Formatter_prototype$format();\n";
    testSets(false, js, output,
        "{format=[[Formatter.prototype], [Unrelated.prototype]]}");

    // TODO(nicksantos): Fix the type tightener to handle this case.
    // It currently doesn't work, because getSubTypes is broken for enums.
  }

  public void testEnumOfObjects2() {
    String js = ""
        + "/** @constructor */ function Formatter() {}"
        + "Formatter.prototype.format = function() {};"
        + "/** @constructor */ function Unrelated() {}"
        + "Unrelated.prototype.format = function() {};"
        + "/** @enum {?Formatter} */ var Enum = {\n"
        + "  A: new Formatter(),\n"
        + "  B: new Formatter()\n"
        + "};\n"
        + "function f() {\n"
        + "  var formatter = window.toString() ? Enum.A : Enum.B;\n"
        + "  formatter.format();\n"
        + "}";
    String output = ""
        + "/** @constructor */ function Formatter() {}"
        + "Formatter.prototype.format = function() {};"
        + "/** @constructor */ function Unrelated() {}"
        + "Unrelated.prototype.format = function() {};"
        + "/** @enum {?Formatter} */ var Enum = {\n"
        + "  A: new Formatter(),\n"
        + "  B: new Formatter()\n"
        + "};\n"
        + "function f() {\n"
        + "  var formatter = window.toString() ? Enum.A : Enum.B;\n"
        + "  formatter.format();\n"
        + "}";
    testSets(false, js, output, "{}");
  }

  public void testEnumOfObjects3() {
    String js = ""
        + "/** @constructor */ function Formatter() {}"
        + "Formatter.prototype.format = function() {};"
        + "/** @constructor */ function Unrelated() {}"
        + "Unrelated.prototype.format = function() {};"
        + "/** @enum {!Formatter} */ var Enum = {\n"
        + "  A: new Formatter(),\n"
        + "  B: new Formatter()\n"
        + "};\n"
        + "/** @enum {!Enum} */ var SubEnum = {\n"
        + "  C: Enum.A\n"
        + "};\n"
        + "function f() {\n"
        + "  var formatter = SubEnum.C\n"
        + "  formatter.format();\n"
        + "}";
    String output = ""
        + "/** @constructor */ function Formatter() {}"
        + "Formatter.prototype.Formatter_prototype$format = function() {};"
        + "/** @constructor */ function Unrelated() {}"
        + "Unrelated.prototype.Unrelated_prototype$format = function() {};"
        + "/** @enum {!Formatter} */ var Enum = {\n"
        + "  A: new Formatter(),\n"
        + "  B: new Formatter()\n"
        + "};\n"
        + "/** @enum {!Enum} */ var SubEnum = {\n"
        + "  C: Enum.A\n"
        + "};\n"
        + "function f() {\n"
        + "  var formatter = SubEnum.C\n"
        + "  formatter.Formatter_prototype$format();\n"
        + "}";
    testSets(false, js, output,
        "{format=[[Formatter.prototype], [Unrelated.prototype]]}");
  }

  public void testUntypedExterns() {
    String externs =
        BaseJSTypeTestCase.ALL_NATIVE_EXTERN_TYPES
        + "var window;"
        + "window.alert = function() {x};";
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.a = 0;\n"
        + "Foo.prototype.alert = 0;\n"
        + "Foo.prototype.window = 0;\n"
        + "/** @constructor */ function Bar() {}\n"
        + "Bar.prototype.a = 0;\n"
        + "Bar.prototype.alert = 0;\n"
        + "Bar.prototype.window = 0;\n"
        + "window.alert();";
    String output = ""
        + "function Foo(){}"
        + "Foo.prototype.Foo_prototype$a=0;"
        + "Foo.prototype.alert=0;"
        + "Foo.prototype.Foo_prototype$window=0;"
        + "function Bar(){}"
        + "Bar.prototype.Bar_prototype$a=0;"
        + "Bar.prototype.alert=0;"
        + "Bar.prototype.Bar_prototype$window=0;"
        + "window.alert();";

    testSets(false, externs, js, output, "{a=[[Bar.prototype], [Foo.prototype]]"
             + ", window=[[Bar.prototype], [Foo.prototype]]}");
    testSets(true, externs, js, output, "{a=[[Bar.prototype], [Foo.prototype]],"
             + " window=[[Bar.prototype], [Foo.prototype]]}");
  }

  public void testUnionTypeInvalidation() {
    String externs = ""
        + "/** @constructor */ function Baz() {}"
        + "Baz.prototype.a";
    String js = ""
        + "/** @constructor */ function Ind() {this.a=0}\n"
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.a = 0;\n"
        + "/** @constructor */ function Bar() {}\n"
        + "Bar.prototype.a = 0;\n"
        + "/** @type {Foo|Bar} */\n"
        + "var F = new Foo;\n"
        + "F.a = 1\n;"
        + "F = new Bar;\n"
        + "/** @type {Baz} */\n"
        + "var Z = new Baz;\n"
        + "Z.a = 1\n;"
        + "/** @type {Bar|Baz} */\n"
        + "var B = new Baz;\n"
        + "B.a = 1;\n"
        + "B = new Bar;\n";
    // Only the constructor field a of Ind is renamed, as Foo is related to Baz
    // through Bar in the unions Bar|Baz and Foo|Bar.
    String output = ""
        + "function Ind() { this.Ind$a = 0; }"
        + "function Foo() {}"
        + "Foo.prototype.a = 0;"
        + "function Bar() {}"
        + "Bar.prototype.a = 0;"
        + "var F = new Foo;"
        + "F.a = 1;"
        + "F = new Bar;"
        + "var Z = new Baz;"
        + "Z.a = 1;"
        + "var B = new Baz;"
        + "B.a = 1;"
        + "B = new Bar;";
    String ttOutput = ""
        + "function Ind() { this.Unique$1$a = 0; }"
        + "function Foo() {}"
        + "Foo.prototype.a = 0;"
        + "function Bar() {}"
        + "Bar.prototype.a = 0;"
        + "var F = new Foo;"
        + "F.a = 1;"
        + "F = new Bar;"
        + "var Z = new Baz;"
        + "Z.a = 1;"
        + "var B = new Baz;"
        + "B.a = 1;"
        + "B = new Bar;";
    testSets(false, externs, js, output, "{a=[[Ind]]}");
    testSets(true, externs, js, ttOutput, "{a=[[Unique$1]]}");
  }

  public void testUnionAndExternTypes() {
    String externs = ""
      + "/** @constructor */ function Foo() { }"
      + "Foo.prototype.a = 4;\n";
    String js = ""
      + "/** @constructor */ function Bar() { this.a = 2; }\n"
      + "/** @constructor */ function Baz() { this.a = 3; }\n"
      + "/** @constructor */ function Buz() { this.a = 4; }\n"
      + "/** @constructor */ function T1() { this.a = 3; }\n"
      + "/** @constructor */ function T2() { this.a = 3; }\n"
      + "/** @type {Bar|Baz} */ var b;\n"
      + "/** @type {Baz|Buz} */ var c;\n"
      + "/** @type {Buz|Foo} */ var d;\n"
      + "b.a = 5; c.a = 6; d.a = 7;";
    String output = ""
      + "/** @constructor */ function Bar() { this.a = 2; }\n"
      + "/** @constructor */ function Baz() { this.a = 3; }\n"
      + "/** @constructor */ function Buz() { this.a = 4; }\n"
      + "/** @constructor */ function T1() { this.T1$a = 3; }\n"
      + "/** @constructor */ function T2() { this.T2$a = 3; }\n"
      + "/** @type {Bar|Baz} */ var b;\n"
      + "/** @type {Baz|Buz} */ var c;\n"
      + "/** @type {Buz|Foo} */ var d;\n"
      + "b.a = 5; c.a = 6; d.a = 7;";

    // We are testing the skipping of multiple types caused by unioning with
    // extern types.
    testSets(false, externs, js, output, "{a=[[T1], [T2]]}");
  }

  public void testTypedExterns() {
    String externs = ""
        + "/** @constructor */ function Window() {};\n"
        + "Window.prototype.alert;"
        + "/** @type {Window} */"
        + "var window;";
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.alert = 0;\n"
        + "window.alert('blarg');";
    String output = ""
        + "function Foo(){}"
        + "Foo.prototype.Foo_prototype$alert=0;"
        + "window.alert('blarg');";
    testSets(false, externs, js, output, "{alert=[[Foo.prototype]]}");
    testSets(true, externs, js, output, "{alert=[[Foo.prototype]]}");
  }

  public void testSubtypesWithSameField() {
    String js = ""
        + "/** @constructor */ function Top() {}\n"
        + "/** @constructor \n@extends Top*/ function Foo() {}\n"
        + "Foo.prototype.a;\n"
        + "/** @constructor \n@extends Top*/ function Bar() {}\n"
        + "Bar.prototype.a;\n"
        + "/** @param {Top} top */"
        + "function foo(top) {\n"
        + "  var x = top.a;\n"
        + "}\n"
        + "foo(new Foo);\n"
        + "foo(new Bar);\n";
    testSets(false, js, "{}");
    testSets(true, js, "{a=[[Bar.prototype, Foo.prototype]]}");
  }

  public void testSupertypeReferenceOfSubtypeProperty() {
    String externs = ""
        + "/** @constructor */ function Ext() {}"
        + "Ext.prototype.a;";
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "/** @constructor \n@extends Foo*/ function Bar() {}\n"
        + "Bar.prototype.a;\n"
        + "/** @param {Foo} foo */"
        + "function foo(foo) {\n"
        + "  var x = foo.a;\n"
        + "}\n";
    String result = ""
        + "function Foo() {}\n"
        + "function Bar() {}\n"
        + "Bar.prototype.Bar_prototype$a;\n"
        + "function foo(foo) {\n"
        + "  var x = foo.Bar_prototype$a;\n"
        + "}\n";
    testSets(false, externs, js, result, "{a=[[Bar.prototype]]}");
  }

  public void testObjectLiteralNotRenamed() {
    String js = ""
        + "var F = {a:'a', b:'b'};"
        + "F.a = 'z';";
    testSets(false, js, js, "{}");
    testSets(true, js, js, "{}");
  }

  public void testObjectLiteralReflected() {
    String js = ""
        + "var goog = {};"
        + "goog.reflect = {};"
        + "goog.reflect.object = function(x, y) { return y; };"
        + "/** @constructor */ function F() {}"
        + "/** @type {number} */ F.prototype.foo = 3;"
        + "/** @constructor */ function G() {}"
        + "/** @type {number} */ G.prototype.foo = 3;"
        + "goog.reflect.object(F, {foo: 5});";
    String result = ""
        + "var goog = {};"
        + "goog.reflect = {};"
        + "goog.reflect.object = function(x, y) { return y; };"
        + "function F() {}"
        + "F.prototype.F_prototype$foo = 3;"
        + "function G() {}"
        + "G.prototype.G_prototype$foo = 3;"
        + "goog.reflect.object(F, {F_prototype$foo: 5});";
    testSets(false, js, result, "{foo=[[F.prototype], [G.prototype]]}");
    testSets(true, js, result, "{foo=[[F.prototype], [G.prototype]]}");
  }

  public void testObjectLiteralLends() {
    String js = ""
        + "var mixin = function(x) { return x; };"
        + "/** @constructor */ function F() {}"
        + "/** @type {number} */ F.prototype.foo = 3;"
        + "/** @constructor */ function G() {}"
        + "/** @type {number} */ G.prototype.foo = 3;"
        + "mixin(/** @lends {F.prototype} */ ({foo: 5}));";
    String result = ""
        + "var mixin = function(x) { return x; };"
        + "function F() {}"
        + "F.prototype.F_prototype$foo = 3;"
        + "function G() {}"
        + "G.prototype.G_prototype$foo = 3;"
        + "mixin(/** @lends {F.prototype} */ ({F_prototype$foo: 5}));";
    testSets(false, js, result, "{foo=[[F.prototype], [G.prototype]]}");
    testSets(true, js, result, "{foo=[[F.prototype], [G.prototype]]}");
  }

  public void testClosureInherits() {
    String js = ""
        + "var goog = {};"
        + "/* @param {Function} childCtor Child class.\n"
        + " * @param {Function} parentCtor Parent class. */\n"
        + "goog.inherits = function(childCtor, parentCtor) {\n"
        + "  /** @constructor */\n"
        + "  function tempCtor() {};\n"
        + "  tempCtor.prototype = parentCtor.prototype;\n"
        + "  childCtor.superClass_ = parentCtor.prototype;\n"
        + "  childCtor.prototype = new tempCtor();\n"
        + "  childCtor.prototype.constructor = childCtor;\n"
        + "};"
        + "/** @constructor */ function Top() {}\n"
        + "Top.prototype.f = function() {};"
        + "/** @constructor \n@extends Top*/ function Foo() {}\n"
        + "goog.inherits(Foo, Top);\n"
        + "Foo.prototype.f = function() {"
        + "  Foo.superClass_.f();"
        + "};\n"
        + "/** @constructor \n* @extends Foo */ function Bar() {}\n"
        + "goog.inherits(Bar, Foo);\n"
        + "Bar.prototype.f = function() {"
        + "  Bar.superClass_.f();"
        + "};\n"
        + "(new Bar).f();\n";
    testSets(false, js, "{f=[[Top.prototype]]}");
    testSets(true, js, "{constructor=[[Bar.prototype, Foo.prototype]], "
                 + "f=[[Bar.prototype], [Foo.prototype], [Top.prototype]]}");
  }

  public void testSkipNativeFunctionMethod() {
    String externs = ""
        + "/** @constructor \n @param {*} var_args */"
        + "function Function(var_args) {}"
        + "Function.prototype.call = function() {};";
    String js = ""
        + "/** @constructor */ function Foo(){};"
        + "/** @constructor\n @extends Foo */"
        + "function Bar() { Foo.call(this); };"; // call should not be renamed
    testSame(externs, js, null);
  }

  public void testSkipNativeObjectMethod() {
    String externs = ""
        + "/** @constructor \n @param {*} opt_v */ function Object(opt_v) {}"
        + "Object.prototype.hasOwnProperty;";
    String js = ""
        + "/** @constructor */ function Foo(){};"
        + "(new Foo).hasOwnProperty('x');";
    testSets(false, externs, js, js, "{}");
    testSets(true, externs, js, js, "{}");
  }

  public void testExtendNativeType() {
    String externs = ""
        + "/** @constructor \n @return {string} */"
        + "function Date(opt_1, opt_2, opt_3, opt_4, opt_5, opt_6, opt_7) {}"
        + "/** @override */ Date.prototype.toString = function() {}";
    String js = ""
        + "/** @constructor\n @extends {Date} */ function SuperDate() {};\n"
        + "(new SuperDate).toString();";
    testSets(true, externs, js, js, "{}");
    testSets(false, externs, js, js, "{}");
  }

  public void testStringFunction() {
    // Extern functions are not renamed, but user functions on a native
    // prototype object are.
    String externs = "/**@constructor\n@param {*} opt_str \n @return {string}*/"
         + "function String(opt_str) {};\n"
         + "/** @override \n @return {string} */\n"
         + "String.prototype.toString = function() { };\n";
    String js = ""
         + "/** @constructor */ function Foo() {};\n"
         + "Foo.prototype.foo = function() {};\n"
         + "String.prototype.foo = function() {};\n"
         + "var a = 'str'.toString().foo();\n";
    String output = ""
         + "function Foo() {};\n"
         + "Foo.prototype.Foo_prototype$foo = function() {};\n"
         + "String.prototype.String_prototype$foo = function() {};\n"
         + "var a = 'str'.toString().String_prototype$foo();\n";

    testSets(false, externs, js, output,
             "{foo=[[Foo.prototype], [String.prototype]]}");
    testSets(true, externs, js, output,
             "{foo=[[Foo.prototype], [String.prototype]]}");
  }

  public void testUnusedTypeInExterns() {
    String externs = ""
        + "/** @constructor */ function Foo() {};\n"
        + "Foo.prototype.a";
    String js = ""
        + "/** @constructor */ function Bar() {};\n"
        + "Bar.prototype.a;"
        + "/** @constructor */ function Baz() {};\n"
        + "Baz.prototype.a;";
    String output = ""
        + "/** @constructor */ function Bar() {};\n"
        + "Bar.prototype.Bar_prototype$a;"
        + "/** @constructor */ function Baz() {};\n"
        + "Baz.prototype.Baz_prototype$a";
    testSets(false, externs, js, output,
             "{a=[[Bar.prototype], [Baz.prototype]]}");
    testSets(true, externs, js, output,
             "{a=[[Bar.prototype], [Baz.prototype]]}");
  }

  public void testInterface() {
    String js = ""
        + "/** @interface */ function I() {};\n"
        + "I.prototype.a;\n"
        + "/** @constructor \n @implements I */ function Foo() {};\n"
        + "Foo.prototype.a;\n"
        + "/** @type I */\n"
        + "var F = new Foo;"
        + "var x = F.a;";
    testSets(false, js, "{a=[[Foo.prototype, I.prototype]]}");
    testSets(true, js, "{a=[[Foo.prototype], [I.prototype]]}");
  }

  public void testInterfaceOfSuperclass() {
    String js = ""
        + "/** @interface */ function I() {};\n"
        + "I.prototype.a;\n"
        + "/** @constructor \n @implements I */ function Foo() {};\n"
        + "Foo.prototype.a;\n"
        + "/** @constructor \n @extends Foo */ function Bar() {};\n"
        + "Bar.prototype.a;\n"
        + "/** @type Bar */\n"
        + "var B = new Bar;"
        + "B.a = 0";
    testSets(false, js, "{a=[[Foo.prototype, I.prototype]]}");
    testSets(true, js,
        "{a=[[Bar.prototype], [Foo.prototype], [I.prototype]]}");
  }

  public void testTwoInterfacesWithSomeInheritance() {
    String js = ""
        + "/** @interface */ function I() {};\n"
        + "I.prototype.a;\n"
        + "/** @interface */ function I2() {};\n"
        + "I2.prototype.a;\n"
        + "/** @constructor \n @implements I */ function Foo() {};\n"
        + "Foo.prototype.a;\n"
        + "/** @constructor \n @extends Foo \n @implements I2*/\n"
        + "function Bar() {};\n"
        + "Bar.prototype.a;\n"
        + "/** @type Bar */\n"
        + "var B = new Bar;"
        + "B.a = 0";
    testSets(false, js, "{a=[[Foo.prototype, I.prototype, I2.prototype]]}");
    testSets(true, js, "{a=[[Bar.prototype], [Foo.prototype], "
                       + "[I.prototype], [I2.prototype]]}");
  }

  public void testInvalidatingInterface() {
    String js = ""
        + "/** @interface */ function I2() {};\n"
        + "I2.prototype.a;\n"
        + "/** @constructor */ function Bar() {}\n"
        + "/** @type I */\n"
        + "var i = new Bar;\n" // Make I invalidating
        + "/** @constructor \n @implements I \n @implements I2 */"
        + "function Foo() {};\n"
        + "Foo.prototype.a = 0;\n"
        + "(new Foo).a = 0;"
        + "/** @interface */ function I() {};\n"
        + "I.prototype.a;\n";
    testSets(false, js, "{}");
    testSets(true, js, "{}");
  }

  public void testMultipleInterfaces() {
    String js = ""
        + "/** @interface */ function I() {};\n"
        + "/** @interface */ function I2() {};\n"
        + "I2.prototype.a;\n"
        + "/** @constructor \n @implements I \n @implements I2 */"
        + "function Foo() {};\n"
        + "Foo.prototype.a = 0;\n"
        + "(new Foo).a = 0";
    testSets(false, js, "{a=[[Foo.prototype, I2.prototype]]}");
    testSets(true, js, "{a=[[Foo.prototype], [I2.prototype]]}");
  }

  public void testInterfaceWithSupertypeImplementor() {
    String js = ""
        + "/** @interface */ function C() {}\n"
        + "C.prototype.foo = function() {};\n"
        + "/** @constructor */ function A (){}\n"
        + "A.prototype.foo = function() {};\n"
        + "/** @constructor \n @implements {C} \n @extends {A} */\n"
        + "function B() {}\n"
        + "/** @type {C} */ var b = new B();\n"
        + "b.foo();\n";
    testSets(false, js, "{foo=[[A.prototype, C.prototype]]}");
    testSets(true, js, "{foo=[[A.prototype], [C.prototype]]}");
  }

  public void testSuperInterface() {
    String js = ""
        + "/** @interface */ function I() {};\n"
        + "I.prototype.a;\n"
        + "/** @interface \n @extends I */ function I2() {};\n"
        + "/** @constructor \n @implements I2 */"
        + "function Foo() {};\n"
        + "Foo.prototype.a = 0;\n"
        + "(new Foo).a = 0";
    testSets(false, js, "{a=[[Foo.prototype, I.prototype]]}");
    testSets(true, js, "{a=[[Foo.prototype], [I.prototype]]}");
  }

  public void testInterfaceUnionWithCtor() {
    String js = ""
        + "/** @interface */ function I() {};\n"
        + "/** @type {!Function} */ I.prototype.addEventListener;\n"
        + "/** @constructor \n * @implements {I} */ function Impl() {};\n"
        + "/** @type {!Function} */ Impl.prototype.addEventListener;"
        + "/** @constructor */ function C() {};\n"
        + "/** @type {!Function} */ C.prototype.addEventListener;"
        + "/** @param {C|I} x */"
        + "function f(x) { x.addEventListener(); };\n"
        + "f(new C()); f(new Impl());";

    testSets(false, js, js,
        "{addEventListener=[[C.prototype, I.prototype, Impl.prototype]]}");

    // In the tightened case, the disambiguator is smart enough to
    // disambiguate Impl's method from the interface method.
    String tightenedOutput = ""
        + "function I() {};\n"
        + "I.prototype.I_prototype$addEventListener;\n"
        + "function Impl() {};\n"
        + "Impl.prototype.C_prototype$addEventListener;"
        + "function C() {};\n"
        + "C.prototype.C_prototype$addEventListener;"
        + "/** @param {C|I} x */"
        + "function f(x) { x.C_prototype$addEventListener(); };\n"
        + "f(new C()); f(new Impl());";

    testSets(true, js, tightenedOutput,
        "{addEventListener=[[C.prototype, Impl.prototype], [I.prototype]]}");
  }

  public void testExternInterfaceUnionWithCtor() {
    String externs = ""
        + "/** @interface */ function I() {};\n"
        + "/** @type {!Function} */ I.prototype.addEventListener;\n"
        + "/** @constructor \n * @implements {I} */ function Impl() {};\n"
        + "/** @type {!Function} */ Impl.prototype.addEventListener;";

    String js = ""
        + "/** @constructor */ function C() {};\n"
        + "/** @type {!Function} */ C.prototype.addEventListener;"
        + "/** @param {C|I} x */"
        + "function f(x) { x.addEventListener(); };\n"
        + "f(new C()); f(new Impl());";

    testSets(false, externs, js, js, "{}");
    testSets(true, externs, js, js, "{}");
  }

  /**
   * Tests that the type based version skips renaming on types that have a
   * mismatch, and the type tightened version continues to work as normal.
   */
  public void testMismatchInvalidation() {
    String js = ""
        + "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.a = 0;\n"
        + "/** @constructor */ function Bar() {}\n"
        + "Bar.prototype.a = 0;\n"
        + "/** @type Foo */\n"
        + "var F = new Bar;\n"
        + "F.a = 0;";

    testSets(false, "", js, js, "{}", TypeValidator.TYPE_MISMATCH_WARNING,
             "initializing variable\n"
             + "found   : Bar\n"
             + "required: (Foo|null)");
    testSets(true, "", js, js, "{}", TypeValidator.TYPE_MISMATCH_WARNING,
             "initializing variable\n"
             + "found   : Bar\n"
             + "required: (Foo|null)");
  }

  public void testBadCast() {
    String js = "/** @constructor */ function Foo() {};\n"
        + "Foo.prototype.a = 0;\n"
        + "/** @constructor */ function Bar() {};\n"
        + "Bar.prototype.a = 0;\n"
        + "var a = /** @type {!Foo} */ (new Bar);\n"
        + "a.a = 4;";
    testSets(false, "", js, js, "{}",
             TypeValidator.INVALID_CAST,
             "invalid cast - must be a subtype or supertype\n"
             + "from: Bar\n"
             + "to  : Foo");
  }

  public void testDeterministicNaming() {
    String js =
        "/** @constructor */function A() {}\n"
        + "/** @return {string} */A.prototype.f = function() {return 'a';};\n"
        + "/** @constructor */function B() {}\n"
        + "/** @return {string} */B.prototype.f = function() {return 'b';};\n"
        + "/** @constructor */function C() {}\n"
        + "/** @return {string} */C.prototype.f = function() {return 'c';};\n"
        + "/** @type {A|B} */var ab = 1 ? new B : new A;\n"
        + "/** @type {string} */var n = ab.f();\n";

    String output =
        "function A() {}\n"
        + "A.prototype.A_prototype$f = function() { return'a'; };\n"
        + "function B() {}\n"
        + "B.prototype.A_prototype$f = function() { return'b'; };\n"
        + "function C() {}\n"
        + "C.prototype.C_prototype$f = function() { return'c'; };\n"
        + "var ab = 1 ? new B : new A; var n = ab.A_prototype$f();\n";

    for (int i = 0; i < 5; i++) {
      testSets(false, js, output,
          "{f=[[A.prototype, B.prototype], [C.prototype]]}");

      testSets(true, js, output,
          "{f=[[A.prototype, B.prototype], [C.prototype]]}");
    }
  }

  public void testObjectLiteral() {
    String js = "/** @constructor */ function Foo() {}\n"
        + "Foo.prototype.a;\n"
        + "/** @constructor */ function Bar() {}\n"
        + "Bar.prototype.a;\n"
        + "var F = /** @type {Foo} */({ a: 'a' });\n";

    String output = "function Foo() {}\n"
        + "Foo.prototype.Foo_prototype$a;\n"
        + "function Bar() {}\n"
        + "Bar.prototype.Bar_prototype$a;\n"
        + "var F = { Foo_prototype$a: 'a' };\n";

    testSets(false, js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
    testSets(true, js, output, "{a=[[Bar.prototype], [Foo.prototype]]}");
  }

  public void testCustomInherits() {
    String js = "Object.prototype.inheritsFrom = function(shuper) {\n" +
        "  /** @constructor */\n" +
        "  function Inheriter() { }\n" +
        "  Inheriter.prototype = shuper.prototype;\n" +
        "  this.prototype = new Inheriter();\n" +
        "  this.superConstructor = shuper;\n" +
        "};\n" +
        "function Foo(var1, var2, strength) {\n" +
        "  Foo.superConstructor.call(this, strength);\n" +
        "}" +
        "Foo.inheritsFrom(Object);";

    String externs = "" +
        "function Function(var_args) {}" +
        "/** @return {*} */Function.prototype.call = function(var_args) {};";

    testSets(false, externs, js, js, "{}");
  }

  public void testSkipNativeFunctionStaticProperty() {
    String js = ""
      + "/** @param {!Function} ctor */\n"
      + "function addSingletonGetter(ctor) { ctor.a; }\n"
      + "/** @constructor */ function Foo() {}\n"
      + "Foo.a = 0;"
      + "/** @constructor */ function Bar() {}\n"
      + "Bar.a = 0;";

    String output = ""
        + "function addSingletonGetter(ctor){ctor.a}"
        + "function Foo(){}"
        + "Foo.a=0;"
        + "function Bar(){}"
        + "Bar.a=0";

    testSets(false, js, output, "{}");
  }

  public void testErrorOnProtectedProperty() {
    String js = "function addSingletonGetter(foo) { foo.foobar = 'a'; };";

    Compiler compiler = new Compiler();
    CompilerOptions options = new CompilerOptions();
    compiler.init(new JSSourceFile[]{JSSourceFile.fromCode("externs", "")},
                  new JSSourceFile[]{
                      JSSourceFile.fromCode("testcode", js)}, options);

    Node root = compiler.parseInputs();
    Node externsRoot = root.getFirstChild();
    Node mainRoot = externsRoot.getNext();
    getProcessor(compiler).process(externsRoot, mainRoot);

    assertEquals(1, compiler.getErrors().length);
    assertTrue(compiler.getErrors()[0].toString().contains("foobar"));
  }

  public void runFindHighestTypeInChain() {
    // Check that this doesn't go into an infinite loop.
    DisambiguateProperties.forJSTypeSystem(new Compiler(),
        Maps.<String, CheckLevel>newHashMap())
        .getTypeWithProperty("no",
            new JSTypeRegistry(new TestErrorReporter(null, null))
            .getNativeType(JSTypeNative.OBJECT_PROTOTYPE));
  }

  @SuppressWarnings("unchecked")
  private void testSets(boolean runTightenTypes, String js, String expected,
      String fieldTypes) {
    this.runTightenTypes = runTightenTypes;
    test(js, expected);
    assertEquals(
        fieldTypes, mapToString(lastPass.getRenamedTypesForTesting()));
  }

  @SuppressWarnings("unchecked")
  private void testSets(boolean runTightenTypes, String externs, String js,
       String expected, String fieldTypes) {
    testSets(runTightenTypes, externs, js, expected, fieldTypes, null, null);
  }

  @SuppressWarnings("unchecked")
  private void testSets(boolean runTightenTypes, String externs, String js,
       String expected, String fieldTypes, DiagnosticType warning,
       String description) {
    this.runTightenTypes = runTightenTypes;
    test(externs, js, expected, null, warning, description);
    assertEquals(
        fieldTypes, mapToString(lastPass.getRenamedTypesForTesting()));
  }

  /**
   * Compiles the code and checks that the set of types for each field matches
   * the expected value.
   *
   * <p>The format for the set of types for fields is:
   * {field=[[Type1, Type2]]}
   */
  @SuppressWarnings("unchecked")
  private void testSets(boolean runTightenTypes, String js, String fieldTypes) {
    this.runTightenTypes = runTightenTypes;
    Compiler compiler = new Compiler();
    CompilerOptions options = new CompilerOptions();
    compiler.init(new JSSourceFile[]{JSSourceFile.fromCode("externs", "")},
                  new JSSourceFile[]{
                      JSSourceFile.fromCode("testcode", js)}, options);

    Node root = compiler.parseInputs();
    assertTrue("Unexpected parse error(s): " +
        Joiner.on("\n").join(compiler.getErrors()), root != null);

    Node externsRoot = root.getFirstChild();
    Node mainRoot = externsRoot.getNext();
    getProcessor(compiler).process(externsRoot, mainRoot);

    assertEquals(fieldTypes, mapToString(lastPass.getRenamedTypesForTesting()));
  }

  /** Sorts the map and converts to a string for comparision purposes. */
  private <T> String mapToString(Multimap<String, Collection<T>> map) {
    TreeMap<String, String> retMap = Maps.newTreeMap();
    for (String key : map.keySet()) {
      TreeSet<String> treeSet = Sets.newTreeSet();
      for (Collection<T> collection : map.get(key)) {
        Set<String> subSet = Sets.newTreeSet();
        for (T type : collection) {
          subSet.add(type.toString());
        }
        treeSet.add(subSet.toString());
      }
      retMap.put(key, treeSet.toString());
    }
    return retMap.toString();
  }
}
