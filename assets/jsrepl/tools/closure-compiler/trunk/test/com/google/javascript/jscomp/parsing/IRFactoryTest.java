/*
 * Copyright 2009 The Closure Compiler Authors.
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

package com.google.javascript.jscomp.parsing;

import com.google.common.collect.Sets;
import com.google.javascript.jscomp.mozilla.rhino.CompilerEnvirons;
import com.google.javascript.jscomp.mozilla.rhino.Parser;
import com.google.javascript.jscomp.mozilla.rhino.ast.AstRoot;
import com.google.javascript.jscomp.parsing.Config.LanguageMode;
import com.google.javascript.jscomp.testing.TestErrorReporter;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.testing.BaseJSTypeTestCase;

/**
 * Tests {@link IRFactory}.
 */
public class IRFactoryTest extends BaseJSTypeTestCase {

  private LanguageMode mode = LanguageMode.ECMASCRIPT3;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mode = LanguageMode.ECMASCRIPT3;
  }

  public void testScript() throws Exception {
    parse("");
  }

  public void testStrictScript() throws Exception {
    assertNull(newParse("").getDirectives());
    assertEquals(
        Sets.newHashSet("use strict"),
        newParse("'use strict'").getDirectives());
  }

  public void testName() throws Exception {
    parse("a");
  }

  public void testArrayLiteral() throws Exception {
    parse("[a, b]");
  }

  public void testArrayLiteral2() throws Exception {
    testNewParser("[a, , b]",
      "SCRIPT 0 [length: 8]\n" +
      "    EXPR_RESULT 0 [length: 8]\n" +
      "        ARRAYLIT 0 [length: 8]\n" +
      "            NAME a 0 [length: 1]\n" +
      "            EMPTY 0 [length: 1]\n" +
      "            NAME b 0 [length: 1]\n");
  }

  public void testArrayLiteral3() throws Exception {
    parse("[a, undefined, b]");
  }

  public void testArrayLiteral4() throws Exception {
    testNewParser("[,,,a,,b]",
      "SCRIPT 0 [length: 9]\n" +
      "    EXPR_RESULT 0 [length: 9]\n" +
      "        ARRAYLIT 0 [length: 9]\n" +
      "            EMPTY 0 [length: 1]\n" +
      "            EMPTY 0 [length: 1]\n" +
      "            EMPTY 0 [length: 1]\n" +
      "            NAME a 0 [length: 1]\n" +
      "            EMPTY 0 [length: 1]\n" +
      "            NAME b 0 [length: 1]\n");
  }

  public void testAssignment() throws Exception {
    parse("a = b");
  }

  public void testAssignment2() throws Exception {
    parse("a += b");
  }

  public void testInfix() throws Exception {
    parse("a + b");
  }

  public void testScope() throws Exception {
    parse("{ a; b; c; }");
  }

  public void testConditional() throws Exception {
    parse("a ? b : c");
  }

  public void testEmpty() throws Exception {
    parse(";;");
  }

  public void testIf() throws Exception {
    parse("if (a) { b }");
  }

  public void testIf2() throws Exception {
    parse("if (a) { b } else { c }");
  }

  public void testNumber() throws Exception {
    parse("0");
  }

  public void testNumber2() throws Exception {
    parse("1.2");
  }

  public void testString() throws Exception {
    parse("'a'");
  }

  public void testString2() throws Exception {
    parse("\"a\"");
  }

  public void testUnary() throws Exception {
    parse("-a");
  }

  public void testUnary2() throws Exception {
    parse("a++");
  }

  public void testUnary3() throws Exception {
    parse("++a");
  }

  public void testVar() throws Exception {
    parse("var a = 1");
  }

  public void testVar2() throws Exception {
    parse("var a = 1, b = 2");
  }

  public void testVar3() throws Exception {
    parse("var a, b = 1");
  }

  public void testElementGet() throws Exception {
    parse("a[i]");
  }

  public void testPropertyGet() throws Exception {
    parse("a.b");
  }

  public void testRegexp() throws Exception {
    parse("/ab+c/");
  }

  public void testRegexp2() throws Exception {
    parse("/ab+c/g");
  }

  public void testFunctionCall() throws Exception {
    parse("a()");
  }

  public void testFunctionCall2() throws Exception {
    parse("a(b)");
  }

  public void testFunctionCall3() throws Exception {
    parse("a(b, c)");
  }

  public void testNew() throws Exception {
    parse("new A()");
  }

  public void testNew2() throws Exception {
    parse("new A(b)");
  }

  public void testNew3() throws Exception {
    parse("new A(b, c)");
  }

  public void testFunction() {
    parse("function f() {}");
  }

  public void testFunction2() {
    parse("(function() {})");
  }

  public void testFunction3() {
    parse("function f(a) {}");
  }

  public void testFunction4() {
    parse("(function(a) {})");
  }

  public void testFunction5() {
    parse("function f(a, b) {}");
  }

  public void testFunction6() {
    parse("(function(a, b) {})");
  }

  public void testReturn() {
    parse("(function() {return 1;})");
  }

  public void testReturn2() {
    parse("function f() {return;}");
  }

  public void testReturn3() {
    parse("function f(){return x?1:2}");
  }

  public void testThrow() {
    parse("throw e");
  }

  public void testWith() {
    parse("with (a) { b }");
  }

  public void testObjectLiteral() {
    newParse("var o = {}");
  }

  public void testObjectLiteral2() {
    newParse("var o = {a: 1}");
  }

  public void testObjectLiteral3() {
    newParse("var o = {a: 1, b: 2}");
  }

  public void testObjectLiteral4() {
    newParse("var o = {1: 'a'}");
  }

  public void testObjectLiteral5() {
    newParse("var o = {'a': 'a'}");
  }

  public void testObjectLiteral6() {
    testNewParser("({1: true})",
      "SCRIPT 0 [length: 11]\n" +
      "    EXPR_RESULT 0 [length: 10]\n" +
      "        OBJECTLIT 0 [parenthesized: true] [length: 9]\n" +
      "            STRING 1 0 [quoted: 1] [length: 1]\n" +
      "                TRUE 0 [length: 4]\n");
  }

  public void testObjectLiteral7() {
    mode = LanguageMode.ECMASCRIPT5;

    testNewParser("({get 1() {}})",
        "SCRIPT 0 [length: 14]\n" +
        "    EXPR_RESULT 0 [length: 13]\n" +
        "        OBJECTLIT 0 [parenthesized: true] [length: 12]\n" +
        "            GET 1 0 [quoted: 1] [length: 1]\n" +
        "                FUNCTION  0 [length: 6]\n" +
        "                    NAME  0\n" +
        "                    LP 0\n" +
        "                    BLOCK 0 [length: 2]\n");
  }

  public void testObjectLiteral8() {
    mode = LanguageMode.ECMASCRIPT5;

    testNewParser("({set 1(a) {}})",
        "SCRIPT 0 [length: 15]\n" +
        "    EXPR_RESULT 0 [length: 14]\n" +
        "        OBJECTLIT 0 [parenthesized: true] [length: 13]\n" +
        "            SET 1 0 [quoted: 1] [length: 1]\n" +
        "                FUNCTION  0 [length: 7]\n" +
        "                    NAME  0\n" +
        "                    LP 0\n" +
        "                        NAME a 0 [length: 1]\n" +
        "                    BLOCK 0 [length: 2]\n");
  }


  public void testKeywordLiteral() {
    parse("true");
  }

  public void testWhile() {
    parse("while (!a) { a--; }");
  }

  public void testParen() {
    parse("(a)");
  }

  public void testParen2() {
    parse("(1+1)*2");
  }

  public void testFor() {
    parse("for (var i = 0; i < n; i++) { a(i); }");
  }

  public void testForIn() {
    parse("for (i in a) { b(i); }");
  }

  public void testBreak() {
    parse("while (true) { break; }");
  }

  public void testContinue() {
    parse("while (true) { continue; }");
  }

  public void testDoLoop() {
    parse("do { a() } while (b());");
  }

  // The old and new parser produce different results now with labels, and
  // named breaks and continues, so disable these tests.
  public void testLabel() {
    testNewParser("foo: bar",
        "SCRIPT 0 [length: 8]\n" +
        "    LABEL 0 [length: 4]\n" +
        "        LABEL_NAME foo 0 [length: 4]\n" +
        "        EXPR_RESULT 0 [length: 3]\n" +
        "            NAME bar 0 [length: 3]\n");
  }

  public void testLabel2() {
    testNewParser("l: while (f()) { if (g()) { continue l; } }",
        "SCRIPT 0 [length: 43]\n" +
        "    LABEL 0 [length: 2]\n" +
        "        LABEL_NAME l 0 [length: 2]\n" +
        "        WHILE 0 [length: 40]\n" +
        "            CALL 0 [length: 3]\n" +
        "                NAME f 0 [length: 1]\n" +
        "            BLOCK 0 [length: 28]\n" +
        "                IF 0 [length: 24]\n" +
        "                    CALL 0 [length: 3]\n" +
        "                        NAME g 0 [length: 1]\n" +
        "                    BLOCK 0 [length: 15]\n" +
        "                        CONTINUE 0 [length: 11]\n" +
        "                            LABEL_NAME l 0 [length: 1]\n");
  }

  public void testLabel3() {
    testNewParser("Foo:Bar:X:{ break Bar; }",
        "SCRIPT 0 [length: 24]\n" +
        "    LABEL 0 [length: 4]\n" +
        "        LABEL_NAME Foo 0 [length: 4]\n" +
        "        LABEL 0 [length: 4]\n" +
        "            LABEL_NAME Bar 0 [length: 4]\n" +
        "            LABEL 0 [length: 2]\n" +
        "                LABEL_NAME X 0 [length: 2]\n" +
        "                BLOCK 0 [length: 14]\n" +
        "                    BREAK 0 [length: 10]\n" +
        "                        LABEL_NAME Bar 0 [length: 3]\n");
  }

  public void testNegation1() {
    testNewParser("-a",
        "SCRIPT 0 [length: 2]\n" +
        "    EXPR_RESULT 0 [length: 2]\n" +
        "        NEG 0 [length: 2]\n" +
        "            NAME a 0 [length: 1]\n");
  }

  public void testNegation2() {
    testNewParser("-2",
        "SCRIPT 0 [length: 2]\n" +
        "    EXPR_RESULT 0 [length: 2]\n" +
        "        NUMBER -2.0 0 [length: 1]\n");
  }

  public void testNegation3() {
    testNewParser("1 - -2",
        "SCRIPT 0 [length: 6]\n" +
        "    EXPR_RESULT 0 [length: 6]\n" +
        "        SUB 0 [length: 6]\n" +
        "            NUMBER 1.0 0 [length: 1]\n" +
        "            NUMBER -2.0 0 [length: 1]\n");
  }

  public void testGetter() {
    mode = LanguageMode.ECMASCRIPT5;
    testNewParser("({get a() {}})",
        "SCRIPT 0 [length: 14]\n" +
        "    EXPR_RESULT 0 [length: 13]\n" +
        "        OBJECTLIT 0 [parenthesized: true] [length: 12]\n" +
        "            GET a 0 [length: 1]\n" +
        "                FUNCTION  0 [length: 6]\n" +
        "                    NAME  0\n" +
        "                    LP 0\n" +
        "                    BLOCK 0 [length: 2]\n");
  }

  public void testSetter() {
    mode = LanguageMode.ECMASCRIPT5;
    testNewParser("({set a(x) {}})",
        "SCRIPT 0 [length: 15]\n" +
        "    EXPR_RESULT 0 [length: 14]\n" +
        "        OBJECTLIT 0 [parenthesized: true] [length: 13]\n" +
        "            SET a 0 [length: 1]\n" +
        "                FUNCTION  0 [length: 7]\n" +
        "                    NAME  0\n" +
        "                    LP 0\n" +
        "                        NAME x 0 [length: 1]\n" +
        "                    BLOCK 0 [length: 2]\n");
  }

  public void testSwitch() {
    parse("switch (e) {" +
        "case 'a': a(); break;" +
        "case 'b': b();" +
        "case 'c': c(); }");
  }

  public void testSwitch2() {
    parse("switch (e) { case 'a': a(); break; default: b();}");
  }

  public void testSwitch3() {
    parse("function f(){switch(x){default:case 1:return 2}}");
  }

  public void testDebugger() {
    parse("debugger;");
  }

  public void testDelete1() {
    testNoParseError("delete a.b;");
  }

  public void testDelete2() {
    testNoParseError("delete a['b'];");
  }

  public void testDelete3() {
    // This is allowed in ES3 and ES5, but not in ES5/strict. There
    // is a strict mode check for this.
    testNoParseError("delete a;");
  }

  public void testDelete4() {
    testParseError("delete 'x';",
        "Invalid delete operand. Only properties can be deleted.");
  }

  public void testCommentPositions1() {
    Node root = newParse("/** @param {string} x */function a(x) {};" +
        "/** @param {string} x */function b(x) {}");
    Node a = root.getFirstChild();
    Node b = root.getLastChild();
    assertMarkerPosition(a, 0, 4);
    assertMarkerPosition(b, 0, 45);
  }

  public void testCommentPositions2() {
    Node root = newParse(
        "/* foo \n" +
        "   bar \n" +
        "*/\n" +
        "/** @param {string} x */\n" +
        "function a(x) {};\n" +
        "\n" +
        "/* bar \n" +
        "   foo \n" +
        "   foo */\n" +
        "\n" +
        "/**   @param {string} x */\n" +
        "function b(x) {};");
    assertMarkerPosition(root.getFirstChild(), 3, 4);
    assertMarkerPosition(root.getFirstChild().getNext().getNext(), 10, 6);
  }

   public void testLiteralLocation() {
    Node root = newParse(
        "\nvar d =\n" +
        "    \"foo\";\n" +
        "var e =\n" +
        "    1;\n" +
        "var f = \n" +
        "    1.2;\n" +
        "var g = \n" +
        "    2e5;\n" +
        "var h = \n" +
        "    'bar';\n");

    Node firstStmt = root.getFirstChild();
    Node firstLiteral = firstStmt.getFirstChild().getFirstChild();
    Node secondStmt = firstStmt.getNext();
    Node secondLiteral = secondStmt.getFirstChild().getFirstChild();
    Node thirdStmt = secondStmt.getNext();
    Node thirdLiteral = thirdStmt.getFirstChild().getFirstChild();
    Node fourthStmt = thirdStmt.getNext();
    Node fourthLiteral = fourthStmt.getFirstChild().getFirstChild();
    Node fifthStmt = fourthStmt.getNext();
    Node fifthLiteral = fifthStmt.getFirstChild().getFirstChild();

    assertNodePosition(2, 4, firstLiteral);
    assertNodePosition(4, 4, secondLiteral);
    assertNodePosition(6, 4, thirdLiteral);
    assertNodePosition(8, 4, fourthLiteral);
    assertNodePosition(10, 4, fifthLiteral);
  }

  public void testSwitchLocation() {
    Node root = newParse(
        "\nswitch (a) {\n" +
        "  //{\n" +
        "   case 1:\n" +
        "     b++;\n" +
        "   case 2:\n" +
        "   default:\n" +
        "     b--;\n" +
        "  }\n");

    Node switchStmt = root.getFirstChild();
    Node switchVar = switchStmt.getFirstChild();
    Node firstCase = switchVar.getNext();
    Node caseArg = firstCase.getFirstChild();
    Node caseBody = caseArg.getNext();
    Node caseExprStmt = caseBody.getFirstChild();
    Node incrExpr = caseExprStmt.getFirstChild();
    Node incrVar = incrExpr.getFirstChild();
    Node secondCase = firstCase.getNext();
    Node defaultCase = secondCase.getNext();

    assertNodePosition(1, 0, switchStmt);
    assertNodePosition(1, 8, switchVar);
    assertNodePosition(3, 3, firstCase);
    assertNodePosition(3, 8, caseArg);
    assertNodePosition(3, 3, caseBody);
    assertNodePosition(4, 5, caseExprStmt);
    assertNodePosition(4, 5, incrExpr);
    assertNodePosition(4, 5, incrVar);
    assertNodePosition(5, 3, secondCase);
    assertNodePosition(6, 3, defaultCase);
  }

  public void testFunctionParamLocation() {
    Node root = newParse(
        "\nfunction\n" +
        "     foo(a,\n" +
        "     b,\n" +
        "     c)\n" +
        "{}\n");

    Node function = root.getFirstChild();
    Node functionName = function.getFirstChild();
    Node params = functionName.getNext();
    Node param1 = params.getFirstChild();
    Node param2 = param1.getNext();
    Node param3 = param2.getNext();
    Node body = params.getNext();

    assertNodePosition(1, 0, function);
    assertNodePosition(2, 5, functionName);
    // params corresponds to the LP token.
    // Can't be on a separate line because of inferred
    // semicolons.
    assertNodePosition(2, 8, params);
    assertNodePosition(2, 9, param1);
    assertNodePosition(3, 5, param2);
    assertNodePosition(4, 5, param3);
    assertNodePosition(5, 0, body);
  }

  public void testVarDeclLocation() {
    Node root = newParse(
        "\nvar\n" +
        "    a =\n" +
        "    3\n");
    Node varDecl = root.getFirstChild();
    Node varName = varDecl.getFirstChild();
    Node varExpr = varName.getFirstChild();

    assertNodePosition(1, 0, varDecl);
    assertNodePosition(2, 4, varName);
    assertNodePosition(3, 4, varExpr);
  }

  public void testReturnLocation() {
    Node root = newParse(
        "\nfunction\n" +
        "    foo(\n" +
        "    a,\n" +
        "    b,\n" +
        "    c) {\n" +
        "    return\n" +
        "    4;\n" +
        "}\n");

    Node function = root.getFirstChild();
    Node functionName = function.getFirstChild();
    Node params = functionName.getNext();
    Node body = params.getNext();
    Node returnStmt = body.getFirstChild();
    Node exprStmt = returnStmt.getNext();
    Node returnVal = exprStmt.getFirstChild();

    assertNodePosition(6, 4, returnStmt);
    assertNodePosition(7, 4, exprStmt);
    assertNodePosition(7, 4, returnVal);
  }

  public void testLinenoFor() {
    Node root = newParse(
        "\nfor(\n" +
        ";\n" +
        ";\n" +
        ") {\n" +
        "}\n");

    Node forNode = root.getFirstChild();
    Node initClause= forNode.getFirstChild();
    Node condClause = initClause.getNext();
    Node incrClause = condClause.getNext();

    assertNodePosition(1, 0, forNode);
    assertNodePosition(2, 0, initClause);
    assertNodePosition(3, 0, condClause);
    // TODO(bowdidge) Incorrectly gets charno position when EmptyExpression
    // has its absolute position on the carriage return.  For now, the
    // line number gets reported correctly (on the next line) but the
    // character position is -1, so the overall line/char pair in our tree
    // is -1.
    //assertNodePosition(4, 0, incrClause);
  }

  public void testBinaryExprLocation() {
    Node root = newParse(
        "\nvar d = a\n" +
        "    + \n" +
        "    b;\n" +
        "var\n" +
        "    e =\n" +
        "    a +\n" +
        "    c;\n" +
        "var f = b\n" +
        "    / c;\n");

    Node firstVarDecl = root.getFirstChild();
    Node firstVar = firstVarDecl.getFirstChild();
    Node firstVarAdd = firstVar.getFirstChild();

    Node secondVarDecl = firstVarDecl.getNext();
    Node secondVar = secondVarDecl.getFirstChild();
    Node secondVarAdd = secondVar.getFirstChild();

    Node thirdVarDecl = secondVarDecl.getNext();
    Node thirdVar = thirdVarDecl.getFirstChild();
    Node thirdVarAdd = thirdVar.getFirstChild();

    assertNodePosition(1, 0, firstVarDecl);
    assertNodePosition(1, 4, firstVar);
    assertNodePosition(2, 4, firstVarAdd);
    assertNodePosition(1, 8, firstVarAdd.getFirstChild());
    assertNodePosition(3, 4, firstVarAdd.getLastChild());

    assertNodePosition(4, 0, secondVarDecl);
    assertNodePosition(5, 4, secondVar);
    assertNodePosition(6, 6, secondVarAdd);
    assertNodePosition(6, 4, secondVarAdd.getFirstChild());
    assertNodePosition(7, 4, secondVarAdd.getLastChild());

    assertNodePosition(8, 0, thirdVarDecl);
    assertNodePosition(8, 4, thirdVar);
    assertNodePosition(9, 4, thirdVarAdd);
    assertNodePosition(8, 8, thirdVarAdd.getFirstChild());
    assertNodePosition(9, 6, thirdVarAdd.getLastChild());
  }

  public void testPrefixLocation() {
    Node root = newParse(
         "\na++;\n" +
         "--\n" +
         "b;\n");

    Node firstStmt = root.getFirstChild();
    Node secondStmt = firstStmt.getNext();
    Node firstOp = firstStmt.getFirstChild();
    Node secondOp = secondStmt.getFirstChild();

    assertNodePosition(1, 0, firstOp);
    assertNodePosition(2, 0, secondOp);
  }

  public void testIfLocation() {
    Node root = newParse(
        "\nif\n" +
        "  (a == 3)\n" +
        "{\n" +
        "  b = 0;\n" +
        "}\n" +
        "  else\n" +
        "{\n" +
        "  c = 1;\n" +
        "}\n");

    Node ifStmt = root.getFirstChild();
    Node eqClause = ifStmt.getFirstChild();
    Node thenClause = eqClause.getNext();
    Node elseClause = thenClause.getNext();

    assertNodePosition(1, 0, ifStmt);
    assertNodePosition(2, 5, eqClause);
    assertNodePosition(3, 0, thenClause);
    assertNodePosition(7, 0, elseClause);
  }

  public void testTryLocation() {
     Node root = newParse(
         "\ntry {\n" +
         "  var x = 1;\n" +
         "} catch\n" +
         "   (err)\n" +
         "{\n" +
         "} finally {\n" +
         "  var y = 2;\n" +
         "}\n");

    Node tryStmt = root.getFirstChild();
    Node tryBlock = tryStmt.getFirstChild();
    Node catchBlock = tryBlock.getNext();
    Node catchVarBlock = catchBlock.getFirstChild();
    Node catchVar = catchVarBlock.getFirstChild();
    Node finallyBlock = catchBlock.getNext();
    Node finallyStmt = finallyBlock.getFirstChild();

    assertNodePosition(1, 0, tryStmt);
    assertNodePosition(1, 4, tryBlock);
    assertNodePosition(3, 2, catchVarBlock);
    assertNodePosition(4, 4, catchVar);
    assertNodePosition(3, 0, catchBlock);
    assertNodePosition(6, 10, finallyBlock);
    assertNodePosition(7, 2, finallyStmt);
  }

  public void testHookLocation() {
    Node root = newParse(
        "\na\n" +
        "?\n" +
        "b\n" +
        ":\n" +
        "c\n" +
        ";\n");

    Node hookExpr = root.getFirstChild().getFirstChild();
    Node condExpr = hookExpr.getFirstChild();
    Node thenExpr = condExpr.getNext();
    Node elseExpr = thenExpr.getNext();

    assertNodePosition(2, 0, hookExpr);
    assertNodePosition(1, 0, condExpr);
    assertNodePosition(3, 0, thenExpr);
    assertNodePosition(5, 0, elseExpr);
  }

  public void testLabelLocation() {
    Node root = newParse(
        "\nfoo:\n" +
        "a = 1;\n" +
        "bar:\n" +
        "b = 2;\n");

    Node firstStmt = root.getFirstChild();
    Node secondStmt = firstStmt.getNext();

    assertNodePosition(1, 0, firstStmt);
    assertNodePosition(3, 0, secondStmt);
  }

  public void testCompareLocation() {
    Node root = newParse(
        "\na\n" +
        "<\n" +
        "b\n");

    Node condClause = root.getFirstChild().getFirstChild();
    Node lhs = condClause.getFirstChild();
    Node rhs = lhs.getNext();

    assertNodePosition(2, 0, condClause);
    assertNodePosition(1, 0, lhs);
    assertNodePosition(3, 0, rhs);
   }

  public void testEqualityLocation() {
    Node root = newParse(
        "\na\n" +
        "==\n" +
        "b\n");

    Node condClause = root.getFirstChild().getFirstChild();
    Node lhs = condClause.getFirstChild();
    Node rhs = lhs.getNext();

    assertNodePosition(2, 0, condClause);
    assertNodePosition(1, 0, lhs);
    assertNodePosition(3, 0, rhs);
  }

  public void testPlusEqLocation() {
    Node root = newParse(
        "\na\n" +
        "+=\n" +
        "b\n");

    Node condClause = root.getFirstChild().getFirstChild();
    Node lhs = condClause.getFirstChild();
    Node rhs = lhs.getNext();

    assertNodePosition(2, 0, condClause);
    assertNodePosition(1, 0, lhs);
    assertNodePosition(3, 0, rhs);
  }

  public void testCommaLocation() {
    Node root = newParse(
        "\na,\n" +
        "b,\n" +
        "c;\n");

    Node statement = root.getFirstChild();
    Node comma1 = statement.getFirstChild();
    Node comma2 = comma1.getFirstChild();
    Node cRef = comma2.getNext();
    Node aRef = comma2.getFirstChild();
    Node bRef = aRef.getNext();

    assertNodePosition(1, 1, comma2);
    assertNodePosition(1, 0, aRef);
    assertNodePosition(2, 0, bRef);
    assertNodePosition(3, 0, cRef);
  }

  public void testRegexpLocation() {
    Node root = newParse(
        "\nvar path =\n" +
        "replace(\n" +
        "/a/g," +
        "'/');\n");

    Node firstVarDecl = root.getFirstChild();
    Node firstVar = firstVarDecl.getFirstChild();
    Node firstInitializer = firstVar.getFirstChild();
    Node callNode = firstVar.getFirstChild();
    Node fnName = callNode.getFirstChild();
    Node regexObject = fnName.getNext();
    Node aString = regexObject.getFirstChild();
    Node endRegexString = regexObject.getNext();

    assertNodePosition(1, 0, firstVarDecl);
    assertNodePosition(1, 4, firstVar);
    assertNodePosition(2, 7, callNode);
    assertNodePosition(2, 0, fnName);
    assertNodePosition(3, 0, regexObject);
    assertNodePosition(3, 0, aString);
    assertNodePosition(3, 5, endRegexString);
  }

  public void testNestedOr() {
    Node root = newParse(
        "\nif (a && \n" +
        "    b() || \n" +
        "    /* comment */\n" +
        "    c) {\n" +
        "}\n"
    );

    Node ifStmt = root.getFirstChild();
    Node orClause = ifStmt.getFirstChild();
    Node andClause = orClause.getFirstChild();
    Node cName = andClause.getNext();

    assertNodePosition(1, 0, ifStmt);
    assertNodePosition(2, 8, orClause);
    assertNodePosition(1, 6, andClause);
    assertNodePosition(4, 4, cName);

  }

  public void testBitwiseOps() {
      Node root = newParse(
        "\nif (a & \n" +
        "    b() | \n" +
        "    /* comment */\n" +
        "    c) {\n" +
        "}\n"
    );

    Node ifStmt = root.getFirstChild();
    Node bitOr = ifStmt.getFirstChild();
    Node bitAnd = bitOr.getFirstChild();
    Node cName = bitAnd.getNext();

    assertNodePosition(1, 0, ifStmt);
    assertNodePosition(2, 8, bitOr);
    assertNodePosition(1, 6, bitAnd);
    assertNodePosition(4, 4, cName);

  }

  public void testObjectLitLocation() {
    Node root = newParse(
        "\nvar foo =\n" +
        "{ \n" +
        "'A' : 'A', \n" +
        "'B' : 'B', \n" +
        "'C' :\n" +
        "    'C' \n" +
        "};\n");

    Node firstVarDecl = root.getFirstChild();
    Node firstVar = firstVarDecl.getFirstChild();
    Node firstObjectLit = firstVar.getFirstChild();
    Node firstKey = firstObjectLit.getFirstChild();
    Node firstValue = firstKey.getFirstChild();

    Node secondKey = firstKey.getNext();
    Node secondValue = secondKey.getFirstChild();

    Node thirdKey = secondKey.getNext();
    Node thirdValue = thirdKey.getFirstChild();

    assertNodePosition(1, 4, firstVar);
    assertNodePosition(2, 0, firstObjectLit);

    assertNodePosition(3, 0, firstKey);
    assertNodePosition(3, 6, firstValue);

    assertNodePosition(4, 0, secondKey);
    assertNodePosition(4, 6, secondValue);

    assertNodePosition(5, 0, thirdKey);
    assertNodePosition(6, 4, thirdValue);
  }

  public void testTryWithoutCatchLocation() {
     Node root = newParse(
         "\ntry {\n" +
         "  var x = 1;\n" +
         "} finally {\n" +
         "  var y = 2;\n" +
         "}\n");

    Node tryStmt = root.getFirstChild();
    Node tryBlock = tryStmt.getFirstChild();
    Node catchBlock = tryBlock.getNext();
    Node finallyBlock = catchBlock.getNext();
    Node finallyStmt = finallyBlock.getFirstChild();

    assertNodePosition(1, 0, tryStmt);
    assertNodePosition(1, 4, tryBlock);
    assertNodePosition(3, 0, catchBlock);
    assertNodePosition(3, 10, finallyBlock);
    assertNodePosition(4, 2, finallyStmt);
  }

  public void testTryWithoutFinallyLocation() {
     Node root = newParse(
         "\ntry {\n" +
         "  var x = 1;\n" +
         "} catch (ex) {\n" +
         "  var y = 2;\n" +
         "}\n");

    Node tryStmt = root.getFirstChild();
    Node tryBlock = tryStmt.getFirstChild();
    Node catchBlock = tryBlock.getNext();
    Node catchStmt = catchBlock.getFirstChild();
    Node exceptionVar = catchStmt.getFirstChild();
    Node exceptionBlock = exceptionVar.getNext();
    Node varDecl = exceptionBlock.getFirstChild();

    assertNodePosition(1, 0, tryStmt);
    assertNodePosition(1, 4, tryBlock);
    assertNodePosition(3, 0, catchBlock);
    assertNodePosition(3, 2, catchStmt);
    assertNodePosition(3, 9, exceptionVar);
    assertNodePosition(3, 13, exceptionBlock);
    assertNodePosition(4, 2, varDecl);
  }

  public void testMultilineEqLocation() {
    Node  root = newParse(
        "\nif\n" +
        "    (((a == \n" +
        "  3) && \n" +
        "  (b == 2)) || \n" +
        " (c == 1)) {\n" +
        "}\n");
    Node ifStmt = root.getFirstChild();
    Node orTest = ifStmt.getFirstChild();
    Node andTest = orTest.getFirstChild();
    Node cTest = andTest.getNext();
    Node aTest = andTest.getFirstChild();
    Node bTest = aTest.getNext();

    assertNodePosition(1, 0, ifStmt);
    assertNodePosition(4, 12, orTest);
    assertNodePosition(3, 5, andTest);
    assertNodePosition(2, 9, aTest);
    assertNodePosition(4, 5, bTest);
    assertNodePosition(5, 4, cTest);
  }

  public void testMultilineBitTestLocation() {
    Node root = newParse(
        "\nif (\n" +
        "      ((a \n" +
        "        | 3 \n" +
        "       ) == \n" +
        "       (b \n" +
        "        & 2)) && \n" +
        "      ((a \n" +
        "         ^ 0xffff) \n" +
        "       != \n" +
        "       (c \n" +
        "        << 1))) {\n" +
        "}\n");

    Node ifStmt = root.getFirstChild();
    Node andTest = ifStmt.getFirstChild();
    Node eqTest = andTest.getFirstChild();
    Node notEqTest = eqTest.getNext();

    Node bitOrTest = eqTest.getFirstChild();
    Node bitAndTest = bitOrTest.getNext();

    Node bitXorTest = notEqTest.getFirstChild();
    Node bitShiftTest = bitXorTest.getNext();

    assertNodePosition(1, 0, ifStmt);

    assertNodePosition(4, 9, eqTest);
    assertNodePosition(9, 7, notEqTest);

    assertNodePosition(3, 8, bitOrTest);
    assertNodePosition(6, 8, bitAndTest);
    assertNodePosition(8, 9, bitXorTest);
    assertNodePosition(11, 8, bitShiftTest);
  }

  public void testCallLocation() {
    Node root = newParse(
        "\na.\n" +
        "b.\n" +
        "cccc(1);\n");

    Node exprStmt = root.getFirstChild();
    Node functionCall = exprStmt.getFirstChild();
    Node functionProp = functionCall.getFirstChild();
    Node firstNameComponent = functionProp.getFirstChild();
    Node lastNameComponent = firstNameComponent.getNext();

    assertNodePosition(3, 4, functionCall);
    // TODO(bowdidge) New Rhino doesn't keep the position of the dot handy.
    // New Rhino treats the location of the qualified name as the beginning of
    // the whole name.
    // assertNodePosition(1, 0, firstNameComponent);
    assertNodePosition(3, 0, lastNameComponent);
  }

  public void testLinenoDeclaration() {
    Node root = newParse(
        "\na.\n" +
        "b=\n" +
        "function() {};\n");

    Node exprStmt = root.getFirstChild();
    Node fnAssignment =  exprStmt.getFirstChild();
    Node aDotbName = fnAssignment.getFirstChild();
    Node aName = aDotbName.getFirstChild();
    Node bName = aName.getNext();
    Node fnNode = aDotbName.getNext();
    Node fnName = fnNode.getFirstChild();

    assertNodePosition(2, 1, fnAssignment);
    // TODO(bowdidge) New Rhino doesn't keep track of the position of the dot.
    //assertNodePosition(1, 1, aDotbName);
    assertNodePosition(1, 0, aName);
    assertNodePosition(2, 0, bName);
    assertNodePosition(3, 0, fnNode);
    assertNodePosition(3, 8, fnName);
  }

  final String INVALID_ASSIGNMENT_TARGET = "invalid assignment target";
  final String INVALID_INCREMENT_TARGET = "invalid increment target";
  final String INVALID_DECREMENT_TARGET = "invalid decrement target";

  final String INVALID_INC_OPERAND = "Invalid increment operand";
  final String INVALID_DEC_OPERAND = "Invalid decrement operand";

  public void testAssignmentValidation() {
    testNoParseError("x=1");
    testNoParseError("x.y=1");
    testNoParseError("f().y=1");
    testParseError("(x||y)=1", INVALID_ASSIGNMENT_TARGET);
    testParseError("(x?y:z)=1", INVALID_ASSIGNMENT_TARGET);
    testParseError("f()=1", INVALID_ASSIGNMENT_TARGET);

    testNoParseError("x+=1");
    testNoParseError("x.y+=1");
    testNoParseError("f().y+=1");
    testParseError("(x||y)+=1", INVALID_ASSIGNMENT_TARGET);
    testParseError("(x?y:z)+=1", INVALID_ASSIGNMENT_TARGET);
    testParseError("f()+=1", INVALID_ASSIGNMENT_TARGET);

    testParseError("f()++", INVALID_INCREMENT_TARGET);
    testParseError("f()--", INVALID_DECREMENT_TARGET);
    testParseError("++f()", INVALID_INCREMENT_TARGET);
    testParseError("--f()", INVALID_DECREMENT_TARGET);
  }

  private void testNoParseError(String string) {
    testParseError(string, (String)null);
  }

  private void testParseError(String string, String error) {
    testParseError(string, error == null ? null : new String[] { error });
  }

  private void testParseError(String string, String[] errors) {
    Node root = newParse(string, new TestErrorReporter(errors, null));
    assertTrue("unexpected warnings reported",
        errorReporter.hasEncounteredAllWarnings());
    assertTrue("expected error were not reported",
        errorReporter.hasEncounteredAllErrors());
  }

  private void assertMarkerPosition(Node n, int lineno, int charno) {
    int count = 0;
    for (JSDocInfo.Marker marker : n.getJSDocInfo().getMarkers()) {
      assertEquals(lineno, marker.getAnnotation().getStartLine());
      assertEquals(charno, marker.getAnnotation().getPositionOnStartLine());
      count++;
    }
    assertEquals(1, count);
  }

  private void assertNodePosition(int lineno, int charno, Node n) {
    assertEquals("Line number", lineno, n.getLineno());
    assertEquals("Column position", charno, n.getCharno());
  }

  private void testNewParser(String code, String expected) {
    String actual = newParse(code).toStringTree();
    assertEquals(expected, actual);
  }

  private void parse(String string) {
    String compare = newParse(string).checkTreeEquals(oldParse(string));
    assertTrue(compare, compare == null);
  }

  private Node newParse(String string) {
    return newParse(string, new TestErrorReporter(null, null));
  }

  private Node newParse(String string, TestErrorReporter errorReporter) {
    CompilerEnvirons environment = new CompilerEnvirons();

    environment.setRecordingComments(true);
    environment.setRecordingLocalJsDocComments(true);

    Parser p = new Parser(environment);
    AstRoot script = p.parse(string, null, 0);

    Config config = ParserRunner.createConfig(true, mode, false);
    Node root = IRFactory.transformTree(
        script, null, string, config, errorReporter);

    return root;
  }

  private Node oldParse(String string) {
    com.google.javascript.rhino.CompilerEnvirons environment =
        new com.google.javascript.rhino.CompilerEnvirons();

    environment.setParseJSDoc(true);

    com.google.javascript.rhino.Parser p =
        new com.google.javascript.rhino.Parser(environment, null);

    Node root = p.parse(string, null, 0);

    return root;
  }
}
