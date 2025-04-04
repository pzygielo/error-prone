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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.isStatic;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.lang.model.element.Name;

@BugPattern(
    summary = "Type parameter declaration overrides another type parameter already declared",
    severity = WARNING,
    tags = StandardTags.STYLE)
public class TypeParameterShadowing extends BugChecker
    implements MethodTreeMatcher, ClassTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (tree.getTypeParameters().isEmpty()) {
      return Description.NO_MATCH;
    }
    return findDuplicatesOf(tree, tree.getTypeParameters(), state);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (tree.getTypeParameters().isEmpty()) {
      return Description.NO_MATCH;
    }
    return findDuplicatesOf(tree, tree.getTypeParameters(), state);
  }

  private Description findDuplicatesOf(
      Tree tree, List<? extends TypeParameterTree> typeParameters, VisitorState state) {

    Symbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol == null) {
      return Description.NO_MATCH;
    }

    List<TypeVariableSymbol> enclosingTypeSymbols = typeVariablesEnclosing(symbol);
    if (enclosingTypeSymbols.isEmpty()) {
      return Description.NO_MATCH;
    }

    // See if the passed in list typeParameters exist in the enclosingTypeSymbols
    List<TypeVariableSymbol> conflictingTypeSymbols = new ArrayList<>();
    typeParameters.forEach(
        param ->
            enclosingTypeSymbols.stream()
                .filter(tvs -> tvs.name.contentEquals(param.getName()))
                .findFirst()
                .ifPresent(conflictingTypeSymbols::add));
    if (conflictingTypeSymbols.isEmpty()) {
      return Description.NO_MATCH;
    }

    // Describes what's the conflicting type and where it is
    Description.Builder descriptionBuilder = buildDescription(tree);
    String message =
        "Found aliased type parameters: "
            + conflictingTypeSymbols.stream()
                .map(tvs -> tvs.name + " declared in " + tvs.owner.getSimpleName())
                .collect(Collectors.joining("\n"));

    descriptionBuilder.setMessage(message);

    // Map conflictingTypeSymbol to its new name
    ImmutableSet<String> typeVarsInScope =
        Streams.concat(enclosingTypeSymbols.stream(), symbol.getTypeParameters().stream())
            .map(v -> v.name.toString())
            .collect(toImmutableSet());
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    conflictingTypeSymbols.stream()
        .map(
            v ->
                SuggestedFixes.renameTypeParameter(
                    typeParameterInList(typeParameters, v),
                    tree,
                    replacementTypeVarName(v.name, typeVarsInScope),
                    state))
        .forEach(fixBuilder::merge);

    descriptionBuilder.addFix(fixBuilder.build());
    return descriptionBuilder.build();
  }

  // Package-private for TypeNameShadowing
  static TypeParameterTree typeParameterInList(
      List<? extends TypeParameterTree> typeParameters, Symbol v) {
    return typeParameters.stream()
        .filter(t -> t.getName().contentEquals(v.name))
        .collect(MoreCollectors.onlyElement());
  }

  private static final Pattern TRAILING_DIGIT_EXTRACTOR = Pattern.compile("^(.*?)(\\d+)$");

  // T -> T2
  // T2 -> T3
  // T -> T4 (if T2 and T3 already exist)
  // Package-private for TypeNameShadowing
  static String replacementTypeVarName(Name name, Set<String> superTypeVars) {
    String baseName = name.toString();
    int typeVarNum = 2;

    Matcher matcher = TRAILING_DIGIT_EXTRACTOR.matcher(name);
    if (matcher.matches()) {
      baseName = matcher.group(1);
      typeVarNum = Integer.parseInt(matcher.group(2)) + 1;
    }

    String replacementName;
    while (superTypeVars.contains(replacementName = baseName + typeVarNum)) {
      typeVarNum++;
    }

    return replacementName;
  }

  // Get list of type params of every enclosing class
  private static List<TypeVariableSymbol> typeVariablesEnclosing(Symbol sym) {
    List<TypeVariableSymbol> typeVarScopes = new ArrayList<>();
    outer:
    while (!isStatic(sym)) {
      sym = sym.owner;
      switch (sym.getKind()) {
        case PACKAGE -> {
          break outer;
        }
        case METHOD, CLASS -> typeVarScopes.addAll(sym.getTypeParameters());
        default -> {}
      }
    }
    return typeVarScopes;
  }
}
