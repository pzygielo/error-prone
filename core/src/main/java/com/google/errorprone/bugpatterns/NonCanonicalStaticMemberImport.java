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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ImportTreeMatcher;
import com.google.errorprone.bugpatterns.StaticImports.StaticImportInfo;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ImportTree;

/**
 * Members shouldn't be statically imported by their non-canonical name.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
    summary = "Static import of member uses non-canonical name",
    severity = WARNING,
    documentSuppression = false)
public class NonCanonicalStaticMemberImport extends BugChecker implements ImportTreeMatcher {

  @Override
  public Description matchImport(ImportTree tree, VisitorState state) {
    StaticImportInfo importInfo = StaticImports.tryCreate(tree, state);
    if (importInfo == null || importInfo.isCanonical() || importInfo.members().isEmpty()) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .addFix(SuggestedFix.replace(tree, importInfo.importStatement()))
        .setMessage(
            String.format(
                "Static import of '%s' uses non-canonical name; prefer '%s'",
                importInfo.simpleName(), importInfo.canonicalName()))
        .build();
  }
}
