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

package com.google.errorprone.bugpatterns.inject;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author glorioso@google.com (Nick Glorioso)
 */
@RunWith(JUnit4.class)
public class QualifierWithTypeUseTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(QualifierWithTypeUse.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "QualifierWithTypeUsePositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.inject.testdata;

            import com.google.inject.BindingAnnotation;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import javax.inject.Qualifier;

            /** Tests for {@code QualifierWithTypeUse} */
            public class QualifierWithTypeUsePositiveCases {

              @Qualifier
              // BUG: Diagnostic contains: @Target({CONSTRUCTOR})
              @Target({ElementType.TYPE_USE, ElementType.CONSTRUCTOR})
              @interface Qualifier1 {}

              @Qualifier
              // BUG: Diagnostic contains: remove
              @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
              @interface Qualifier2 {}

              @BindingAnnotation
              // BUG: Diagnostic contains: @Target({FIELD})
              @Target({ElementType.FIELD, ElementType.TYPE_USE})
              @interface BindingAnnotation1 {}

              @BindingAnnotation
              // BUG: Diagnostic contains: remove
              @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
              @interface BindingAnnotation2 {}

              @BindingAnnotation
              // BUG: Diagnostic contains: remove
              @Target(ElementType.TYPE_USE)
              @interface BindingAnnotation3 {}
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "QualifierWithTypeUseNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.inject.testdata;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import javax.inject.Qualifier;

            /** Tests for {@code QualifierWithTypeUse} */
            public class QualifierWithTypeUseNegativeCases {

              @Qualifier
              @Target({ElementType.CONSTRUCTOR})
              @interface Qualifier1 {}

              @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
              @interface NotAQualifier {}
            }\
            """)
        .doTest();
  }
}
