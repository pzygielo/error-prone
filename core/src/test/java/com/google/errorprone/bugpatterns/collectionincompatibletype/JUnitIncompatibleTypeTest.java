/*
 * Copyright 2024 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class JUnitIncompatibleTypeTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnitIncompatibleType.class, getClass());

  @Test
  public void assertEquals_mismatched() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertEquals;
            import static org.junit.Assert.assertNotEquals;

            class Test {
              public void test() {
                // BUG: Diagnostic contains:
                assertEquals(1, "");
                // BUG: Diagnostic contains:
                assertEquals("foo", 1, "");
                // BUG: Diagnostic contains:
                assertEquals("foo", "", 1);
                // BUG: Diagnostic contains:
                assertNotEquals(1, "");
                // BUG: Diagnostic contains:
                assertNotEquals("foo", 1, "");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void assertEquals_matched() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertEquals;
            import static org.junit.Assert.assertNotEquals;

            class Test {
              public void test() {
                assertEquals(1, 2);
                assertEquals(1, 2L);
                assertEquals("foo", 1, 2);
                assertNotEquals(1, 2);
                assertNotEquals("foo", 1, 2);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void assertArrayEquals_mismatched() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertArrayEquals;

            final class Test {
              public void test() {
                // BUG: Diagnostic contains:
                assertArrayEquals(new Test[] {}, new String[] {""});
                // BUG: Diagnostic contains:
                assertArrayEquals("foo", new Test[] {}, new String[] {""});
              }
            }
            """)
        .doTest();
  }

  @Test
  public void assertArrayEquals_primitiveOverloadsFine() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertArrayEquals;

            final class Test {
              public void test() {
                assertArrayEquals(new long[] {1L}, new long[] {2L});
              }
            }
            """)
        .doTest();
  }

  @Test
  public void assertArrayEquals_cast() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertArrayEquals;

            final class Test {
              public void test(Object o, byte[] b) {
                assertArrayEquals((byte[]) o, b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void seesThroughCasts() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertEquals;
            import static org.junit.Assert.assertNotEquals;

            class Test {
              public void test() {
                // BUG: Diagnostic contains:
                assertEquals((Object) 1, (Object) 2L);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void fourArgumentOverload() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertArrayEquals;

            class Test {
              public void test() {
                assertArrayEquals("foo", new double[] {1}, new double[] {2}, 10d);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arrayNullComparison() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertArrayEquals;

            class Test {
              public void test() {
                assertArrayEquals(null, new Object[] {2});
              }
            }
            """)
        .doTest();
  }

  @Test
  public void typeVariables_incompatible() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertArrayEquals;
            import java.util.Map;

            class Test {
              public <T extends String> void test(Map<Integer, T[]> xs) {
                T[] x = xs.get(1);
                // BUG: Diagnostic contains:
                assertArrayEquals(x, new Double[] {1d});
              }
            }
            """)
        .doTest();
  }

  @Test
  public void typeVariables_compatible() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertArrayEquals;
            import java.util.Map;

            class Test {
              public <T extends Double> void test(Map<Integer, T[]> xs) {
                T[] x = xs.get(1);
                assertArrayEquals(x, new Double[] {1d});
              }
            }
            """)
        .doTest();
  }
}
