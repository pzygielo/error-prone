/*
 * Copyright 2023 The Error Prone Authors.
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

/** {@link LabelledBreakTarget}Test */
@RunWith(JUnit4.class)
public class LabelledBreakTargetTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(LabelledBreakTarget.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f() {
                // BUG: Diagnostic contains:
                FOO:
                if (true) {
                  break FOO;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f(Iterable<?> xs) {
                ONE:
                while (true) {
                  break ONE;
                }
                TWO:
                do {
                  break TWO;
                } while (true);
                THREE:
                for (var x : xs) {
                  break THREE;
                }
                FOUR:
                for (int i = 0; i < 10; i++) {
                  break FOUR;
                }
              }
            }
            """)
        .doTest();
  }
}
