/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.DoWhileLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ForLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.WhileLoopTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.WhileLoopTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Assignment where a boolean expression was expected;"
            + " use == if this assignment wasn't expected or add parentheses for clarity.",
    severity = WARNING,
    tags = StandardTags.LIKELY_ERROR)
public class LogicalAssignment extends BugChecker
    implements IfTreeMatcher, WhileLoopTreeMatcher, DoWhileLoopTreeMatcher, ForLoopTreeMatcher {

  @Override
  public Description matchIf(IfTree tree, VisitorState state) {
    return checkCondition(skipOneParen(tree.getCondition()), state);
  }

  @Override
  public Description matchDoWhileLoop(DoWhileLoopTree tree, VisitorState state) {
    return checkCondition(skipOneParen(tree.getCondition()), state);
  }

  @Override
  public Description matchForLoop(ForLoopTree tree, VisitorState state) {
    // for loop condition expressions don't have an extra ParenthesizedTree
    return checkCondition(tree.getCondition(), state);
  }

  @Override
  public Description matchWhileLoop(WhileLoopTree tree, VisitorState state) {
    return checkCondition(skipOneParen(tree.getCondition()), state);
  }

  private static ExpressionTree skipOneParen(ExpressionTree tree) {
    // javac includes a ParenthesizedTree for the mandatory parens in if statement and loop
    // conditions, e.g. in `if (true) {}` the condition is a paren tree containing a literal.
    return tree instanceof ParenthesizedTree parenthesizedTree
        ? parenthesizedTree.getExpression()
        : tree;
  }

  private Description checkCondition(ExpressionTree condition, VisitorState state) {
    if (!(condition instanceof AssignmentTree assign)) {
      return NO_MATCH;
    }
    return buildDescription(condition)
        .addFix(
            SuggestedFix.builder().prefixWith(condition, "(").postfixWith(condition, ")").build())
        .addFix(
            SuggestedFix.replace(
                /* startPos= */ state.getEndPosition(assign.getVariable()),
                /* endPos= */ getStartPosition(assign.getExpression()),
                " == "))
        .build();
  }
}
