/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;

/**
 * Matches a {@code return} statement whose returned expression is matched by the given matcher.
 *
 * @author schmitt@google.com (Peter Schmitt)
 */
public class Returns implements Matcher<StatementTree> {

  private final Matcher<? super ExpressionTree> returnedMatcher;

  /**
   * New matcher for a {@code return} statement where the returned expression is matched by the
   * passed {@code returnedMatcher}.
   */
  public Returns(Matcher<? super ExpressionTree> returnedMatcher) {
    this.returnedMatcher = returnedMatcher;
  }

  @Override
  public boolean matches(StatementTree expressionTree, VisitorState state) {
    return expressionTree instanceof ReturnTree returnTree
        && returnedMatcher.matches(returnTree.getExpression(), state);
  }
}
