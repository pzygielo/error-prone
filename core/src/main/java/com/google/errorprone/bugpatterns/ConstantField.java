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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Fields with CONSTANT_CASE names should be both static and final",
    severity = SUGGESTION,
    tags = BugPattern.StandardTags.STYLE)
public class ConstantField extends BugChecker implements VariableTreeMatcher {

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    Symbol.VarSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym.getKind() != ElementKind.FIELD) {
      return Description.NO_MATCH;
    }
    String name = sym.getSimpleName().toString();
    if (sym.isStatic() && sym.getModifiers().contains(Modifier.FINAL)) {
      return Description.NO_MATCH;
    }
    if (!name.equals(Ascii.toUpperCase(name))) {
      return Description.NO_MATCH;
    }

    Description.Builder descriptionBuilder = buildDescription(tree);
    if (canBecomeStaticMember(sym)) {
      SuggestedFixes.addModifiers(tree, state, Modifier.FINAL, Modifier.STATIC)
          .map(
              f ->
                  SuggestedFix.builder()
                      .setShortDescription("make static and final")
                      .merge(f)
                      .build())
          .ifPresent(descriptionBuilder::addFix);
    }
    return descriptionBuilder
        .addFix(
            SuggestedFix.builder()
                .setShortDescription("change to camelcase")
                .merge(
                    SuggestedFixes.renameVariable(
                        tree, CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name), state))
                .build())
        .build();
  }

  private static boolean canBecomeStaticMember(VarSymbol sym) {
    // JLS 8.1.3: It is a compile-time error if an inner class declares a member that is
    // explicitly or implicitly static, unless the member is a constant variable (§4.12.4).

    // We could try and figure out if the declaration *would* be a compile time constant if made
    // static, but that's a bit much to keep adding this fix.
    ClassSymbol owningClass = sym.enclClass();

    // Enum anonymous classes aren't considered isInner() even though they can't hold static fields
    return switch (owningClass.getNestingKind()) {
      case LOCAL, ANONYMOUS -> false;
      default -> !owningClass.isInner();
    };
  }
}
