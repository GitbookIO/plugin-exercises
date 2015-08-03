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
package com.google.javascript.jscomp;

import com.google.common.base.Preconditions;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.regex.Pattern;

/**
 * Generates goog.exportSymbol for test functions, so they can be recognized
 * by the test runner, even if the code is compiled.
 *
 */
class ExportTestFunctions implements CompilerPass {

  private static final Pattern TEST_FUNCTIONS_NAME_PATTERN =
      Pattern.compile("^(?:((\\w+\\.)+prototype\\.)*" +
                      "(setUpPage|setUp|tearDown|tearDownPage|test\\w+))$");

  private AbstractCompiler compiler;
  private final String exportSymbolFunction;
  private final String exportPropertyFunction;

  /**
   * Creates a new export test functions compiler pass.
   * @param compiler
   * @param exportSymbolFunction The function name used to export symbols in JS.
   * @param exportSymbolFunction The function name used to export properties in
   *     JS.
   */
  ExportTestFunctions(AbstractCompiler compiler,
      String exportSymbolFunction, String exportPropertyFunction) {

    Preconditions.checkNotNull(compiler);
    this.compiler = compiler;
    this.exportSymbolFunction = exportSymbolFunction;
    this.exportPropertyFunction = exportPropertyFunction;
  }

  private class ExportTestFunctionsNodes extends
      NodeTraversal.AbstractShallowCallback {

    @Override
    public void visit(NodeTraversal t, Node n, Node parent) {

      if (parent == null) {
        return;
      }

      if (parent.getType() == Token.SCRIPT) {
        if (NodeUtil.isFunctionDeclaration(n)) {
          // Check for a test function statement.
          String functionName = NodeUtil.getFunctionName(n);
          if (isTestFunction(n, functionName)) {
            exportTestFunctionAsSymbol(functionName, n, parent);
          }
        } else if (isVarDeclaredFunction(n)) {
          // Check for a test function expression.
          Node functionNode = n.getFirstChild().getFirstChild();
          String functionName = NodeUtil.getFunctionName(functionNode);
          if (isTestFunction(functionNode, functionName)) {
            exportTestFunctionAsSymbol(functionName, n, parent);
          }
        }
      } else if (NodeUtil.isExprAssign(parent) &&
            n.getLastChild().getType() != Token.ASSIGN) {
        // Check for a test method assignment.
        Node grandparent = parent.getParent();
        if (grandparent != null && grandparent.getType() == Token.SCRIPT) {
          String functionName = n.getFirstChild().getQualifiedName();
          if (isTestFunction(n, functionName)) {
            exportTestFunctionAsProperty(functionName, parent, n, grandparent);
          }
        }
      }
    }

    /**
     * Whether node corresponds to a function expression declared with var,
     * which is of the form:
     * <pre>
     * var functionName = function() {
     *   // Implementation
     * };
     * </pre>
     * This has the AST structure VAR -> NAME -> FUNCTION
     * @param node
     */
    private boolean isVarDeclaredFunction(Node node) {
      if (node.getType() != Token.VAR) {
        return false;
      }
      Node grandchild = node.getFirstChild().getFirstChild();
      return grandchild != null && grandchild.getType() == Token.FUNCTION;
    }
  }

  @Override
  public void process(Node externs, Node root) {
    NodeTraversal.traverse(compiler, root, new ExportTestFunctionsNodes());
  }

  // Adds exportSymbol(testFunctionName, testFunction);
  private void exportTestFunctionAsSymbol(String testFunctionName, Node node,
      Node scriptNode) {

    Node exportCallTarget = NodeUtil.newQualifiedNameNode(
        compiler.getCodingConvention(),
        exportSymbolFunction, node, testFunctionName);
    Node call = new Node(Token.CALL, exportCallTarget);
    if (exportCallTarget.getType() == Token.NAME) {
      call.putBooleanProp(Node.FREE_CALL, true);
    }
    call.addChildToBack(Node.newString(testFunctionName));
    call.addChildToBack(NodeUtil.newQualifiedNameNode(
        compiler.getCodingConvention(),
        testFunctionName, node, testFunctionName));

    Node expression = new Node(Token.EXPR_RESULT, call);

    scriptNode.addChildAfter(expression, node);
    compiler.reportCodeChange();
  }


  // Adds exportProperty() of the test function name on the prototype object
  private void exportTestFunctionAsProperty(String fullyQualifiedFunctionName,
      Node parent, Node node, Node scriptNode) {

    String testFunctionName =
        NodeUtil.getPrototypePropertyName(node.getFirstChild());
    String objectName = fullyQualifiedFunctionName.substring(0,
        fullyQualifiedFunctionName.lastIndexOf('.'));
    String exportCallStr = String.format("%s(%s, '%s', %s);",
        exportPropertyFunction, objectName, testFunctionName,
        fullyQualifiedFunctionName);

    Node exportCall = this.compiler.parseSyntheticCode(exportCallStr)
        .removeChildren();
    exportCall.useSourceInfoFromForTree(scriptNode);

    scriptNode.addChildAfter(exportCall, parent);
    compiler.reportCodeChange();
  }


  /**
   * Whether a function is recognized as a test function. We follow the JsUnit
   * convention for naming (functions should start with "test"), and we also
   * check if it has no parameters declared.
   *
   * @param n The function node
   * @param functionName The name of the function
   * @return {@code true} if the function is recognized as a test function.
   */
  private boolean isTestFunction(Node n, String functionName) {
    return !(functionName == null
        || !TEST_FUNCTIONS_NAME_PATTERN.matcher(functionName).matches());
  }
}
