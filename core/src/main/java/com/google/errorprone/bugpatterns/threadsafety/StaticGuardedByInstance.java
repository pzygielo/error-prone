/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.errorprone.util.ASTHelpers.isStatic;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.SynchronizedTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Map;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "StaticGuardedByInstance",
    summary = "Writes to static fields should not be guarded by instance locks",
    severity = SeverityLevel.WARNING,
    tags = StandardTags.FRAGILE_CODE)
public class StaticGuardedByInstance extends BugChecker implements SynchronizedTreeMatcher {
  @Override
  public Description matchSynchronized(SynchronizedTree tree, VisitorState state) {
    Symbol lock = ASTHelpers.getSymbol(stripParentheses(tree.getExpression()));
    if (!(lock instanceof VarSymbol)) {
      return Description.NO_MATCH;
    }
    if (isStatic(lock)) {
      return Description.NO_MATCH;
    }
    Multimap<VarSymbol, Tree> writes = WriteVisitor.scan(tree.getBlock());
    for (Map.Entry<VarSymbol, Tree> write : writes.entries()) {
      if (!write.getKey().isStatic()) {
        continue;
      }
      state.reportMatch(
          buildDescription(write.getValue())
              .setMessage(
                  String.format(
                      "Write to static variable should not be guarded by instance lock '%s'", lock))
              .build());
    }
    return Description.NO_MATCH;
  }

  static class WriteVisitor extends TreeScanner<Void, Void> {

    static SetMultimap<VarSymbol, Tree> scan(Tree tree) {
      WriteVisitor visitor = new WriteVisitor();
      tree.accept(visitor, null);
      return visitor.writes;
    }

    private final SetMultimap<VarSymbol, Tree> writes = LinkedHashMultimap.create();

    private void recordWrite(ExpressionTree variable) {
      Symbol sym = ASTHelpers.getSymbol(variable);
      if (sym instanceof VarSymbol varSymbol) {
        writes.put(varSymbol, variable);
      }
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void unused) {
      recordWrite(node.getVariable());
      return super.visitAssignment(node, null);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void unused) {
      recordWrite(node.getVariable());
      return super.visitCompoundAssignment(node, null);
    }

    @Override
    public Void visitUnary(UnaryTree node, Void unused) {
      switch (node.getKind()) {
        case PREFIX_DECREMENT, PREFIX_INCREMENT, POSTFIX_DECREMENT, POSTFIX_INCREMENT ->
            recordWrite(node.getExpression());
        default -> {}
      }
      return super.visitUnary(node, null);
    }

    @Override
    public Void visitSynchronized(SynchronizedTree node, Void unused) {
      // don't descend into nested synchronized blocks
      return null;
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void unused) {
      // don't descend into nested synchronized blocks
      return null;
    }
  }
}
