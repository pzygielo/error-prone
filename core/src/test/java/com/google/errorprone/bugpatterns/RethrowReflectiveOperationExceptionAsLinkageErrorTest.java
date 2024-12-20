/*
 * Copyright 2021 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link RethrowReflectiveOperationExceptionAsLinkageError}Test */
@RunWith(JUnit4.class)
public class RethrowReflectiveOperationExceptionAsLinkageErrorTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(
          RethrowReflectiveOperationExceptionAsLinkageError.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "RethrowReflectiveOperationExceptionAsLinkageErrorPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            public class RethrowReflectiveOperationExceptionAsLinkageErrorPositiveCases {
              void assertionErrorExceptionConstructor() {
                try {
                  throw new ReflectiveOperationException();
                } catch (ReflectiveOperationException e) {
                  // BUG: Diagnostic contains: throw new LinkageError(e.getMessage(), e);
                  throw new AssertionError(e);
                }
              }

              void assertionErrorStringConstructor() {
                try {
                  throw new ReflectiveOperationException();
                } catch (ReflectiveOperationException e) {
                  // BUG: Diagnostic contains: throw new LinkageError("Test", e);
                  throw new AssertionError("Test", e);
                }
              }

              void assertionErrorStringFormatConstructor() {
                try {
                  throw new ReflectiveOperationException();
                } catch (ReflectiveOperationException e) {
                  // BUG: Diagnostic contains: throw new LinkageError(String.format("Test"), e);
                  throw new AssertionError(String.format("Test"), e);
                }
              }

              void multiLineCatchBlock() {
                try {
                  throw new ReflectiveOperationException();
                } catch (ReflectiveOperationException e1) {
                  int a = 100;
                  if (a < 100) {
                    try {
                      throw new ReflectiveOperationException();
                    } catch (ReflectiveOperationException e2) {
                      // BUG: Diagnostic contains: throw new LinkageError(e2.getMessage(), e2);
                      throw new AssertionError(e2);
                    }
                  }
                  // BUG: Diagnostic contains: throw new LinkageError(e1.getMessage(), e1);
                  throw new AssertionError(e1);
                }
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "RethrowReflectiveOperationExceptionAsLinkageErrorNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import java.io.IOException;

            public class RethrowReflectiveOperationExceptionAsLinkageErrorNegativeCases {
              void assertionErrorNonStringConstructor() {
                try {
                  throw new ReflectiveOperationException();
                } catch (ReflectiveOperationException e) {
                  throw new AssertionError(1);
                }
              }

              void assertionErrorNoArgConstructor() {
                try {
                  throw new ReflectiveOperationException();
                } catch (ReflectiveOperationException e) {
                  throw new AssertionError();
                }
              }

              void noThrowAssertionError() {
                try {
                  throw new ReflectiveOperationException();
                } catch (ReflectiveOperationException e) {
                  throw new IllegalStateException(e);
                }
              }

              void noCatchReflectiveOperationException() {
                try {
                  throw new ReflectiveOperationException();
                } catch (Exception e) {
                  throw new AssertionError(e);
                }
              }

              void multiCatchExceptions() {
                try {
                  int a = 100;
                  if (a < 100) {
                    throw new IOException("Test");
                  }
                  throw new ReflectiveOperationException();
                } catch (IOException | ReflectiveOperationException e) {
                  throw new AssertionError(e);
                }
              }

              void throwNewReflectiveOperationException() {
                throw new AssertionError(new ReflectiveOperationException("Test"));
              }
            }\
            """)
        .doTest();
  }
}
