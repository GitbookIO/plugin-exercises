/*
 * Copyright 2004 The Closure Compiler Authors.
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
import com.google.javascript.jscomp.NodeTraversal.AbstractPostOrderCallback;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.*;

/**
 * Verifies that constants are only assigned a value once.
 * e.g. var XX = 5;
 * XX = 3;    // error!
 * XX++;      // error!
 *
 */
class ConstCheck extends AbstractPostOrderCallback
    implements CompilerPass {

  static final DiagnosticType CONST_REASSIGNED_VALUE_ERROR =
      DiagnosticType.error(
          "JSC_CONSTANT_REASSIGNED_VALUE_ERROR",
          "constant {0} assigned a value more than once");

  private final AbstractCompiler compiler;
  private final Set<Scope.Var> initializedConstants;

  /**
   * Creates an instance.
   */
  public ConstCheck(AbstractCompiler compiler) {
    this.compiler = compiler;
    this.initializedConstants = new HashSet<Scope.Var>();
  }

  @Override
  public void process(Node externs, Node root) {
    Preconditions.checkState(compiler.getLifeCycleStage().isNormalized());
    NodeTraversal.traverse(compiler, root, this);
  }

  @Override
  public void visit(NodeTraversal t, Node n, Node parent) {
    switch (n.getType()) {
      case Token.NAME:
        if (parent != null &&
            parent.getType() == Token.VAR &&
            n.hasChildren()) {
          String name = n.getString();
          Scope.Var var = t.getScope().getVar(name);
          if (isConstant(var)) {
            if (initializedConstants.contains(var)) {
              reportError(t, n, name);
            } else {
              initializedConstants.add(var);
            }
          }
        }
        break;

      case Token.ASSIGN:
      case Token.ASSIGN_BITOR:
      case Token.ASSIGN_BITXOR:
      case Token.ASSIGN_BITAND:
      case Token.ASSIGN_LSH:
      case Token.ASSIGN_RSH:
      case Token.ASSIGN_URSH:
      case Token.ASSIGN_ADD:
      case Token.ASSIGN_SUB:
      case Token.ASSIGN_MUL:
      case Token.ASSIGN_DIV:
      case Token.ASSIGN_MOD: {
        Node lhs = n.getFirstChild();
        if (lhs.getType() == Token.NAME) {
          String name = lhs.getString();
          Scope.Var var = t.getScope().getVar(name);
          if (isConstant(var)) {
            if (initializedConstants.contains(var)) {
              reportError(t, n, name);
            } else {
              initializedConstants.add(var);
            }
          }
        }
        break;
      }

      case Token.INC:
      case Token.DEC: {
        Node lhs = n.getFirstChild();
        if (lhs.getType() == Token.NAME) {
          String name = lhs.getString();
          Scope.Var var = t.getScope().getVar(name);
          if (isConstant(var)) {
            reportError(t, n, name);
          }
        }
        break;
      }
    }
  }

  /**
   * Gets whether a variable is a constant initialized to a literal value at
   * the point where it is declared.
   */
  private boolean isConstant(Scope.Var var) {
    return var != null && var.isConst();
  }

  /**
   * Reports a reassigned constant error.
   */
  void reportError(NodeTraversal t, Node n, String name) {
    compiler.report(t.makeError(n, CONST_REASSIGNED_VALUE_ERROR, name));
  }
}
