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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.names.NamingConventions.splitToLowercaseTerms;
import static java.util.Collections.disjoint;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Detect whether the method invocation we are examining is enclosed by either a method or a class
 * which is involved in reversing things.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
final class EnclosedByReverseHeuristic implements Heuristic {

  private static final ImmutableSet<String> DEFAULT_REVERSE_WORDS_TERMS =
      ImmutableSet.of(
          "backward",
          "backwards",
          "complement",
          "endian",
          "flip",
          "inverse",
          "invert",
          "landscape",
          "opposite",
          "portrait",
          "reciprocal",
          "reverse",
          "reversed",
          "rotate",
          "rotated",
          "rotation",
          "swap",
          "swapped",
          "transpose",
          "transposed",
          "undo");

  private final ImmutableSet<String> reverseWordsTerms;

  EnclosedByReverseHeuristic() {
    this(DEFAULT_REVERSE_WORDS_TERMS);
  }

  EnclosedByReverseHeuristic(ImmutableSet<String> reverseWordsTerms) {
    this.reverseWordsTerms = reverseWordsTerms;
  }

  /** Return true if this call is not enclosed in a method call about reversing things */
  @Override
  public boolean isAcceptableChange(
      Changes changes, Tree node, MethodSymbol symbol, VisitorState state) {
    return findReverseWordsMatchInParentNodes(state) == null;
  }

  private @Nullable String findReverseWordsMatchInParentNodes(VisitorState state) {
    return stream(state.getPath())
        .flatMap(t -> getName(t).stream())
        .filter(n -> !disjoint(splitToLowercaseTerms(n), reverseWordsTerms))
        .findFirst()
        .orElse(null);
  }

  private static Optional<String> getName(Tree tree) {
    if (tree instanceof MethodTree methodTree) {
      return Optional.of(methodTree.getName().toString());
    }

    if (tree instanceof ClassTree classTree) {
      return Optional.of(classTree.getSimpleName().toString());
    }

    return Optional.empty();
  }
}
