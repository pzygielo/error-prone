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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class FunctionalInterfaceMethodChangedTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FunctionalInterfaceMethodChanged.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "FunctionalInterfaceMethodChangedPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            public class FunctionalInterfaceMethodChangedPositiveCases {
              @FunctionalInterface
              interface SuperFI {
                void superSam();
              }

              @FunctionalInterface
              interface OtherSuperFI {
                void otherSuperSam();
              }

              @FunctionalInterface
              interface SubFI extends SuperFI {
                void subSam();

                @Override
                // BUG: Diagnostic contains:
                default void superSam() {
                  subSam();
                  System.out.println("do something else");
                }
              }

              @FunctionalInterface
              interface MultipleInheritanceSubFIOneBad extends SuperFI, OtherSuperFI {
                void subSam();

                @Override
                default void superSam() {
                  subSam();
                }

                @Override
                // BUG: Diagnostic contains:
                default void otherSuperSam() {
                  subSam();
                  System.out.println("do something else");
                }
              }

              @FunctionalInterface
              interface MultipleInheritanceSubFIBothBad extends SuperFI, OtherSuperFI {
                void subSam();

                @Override
                // BUG: Diagnostic contains:
                default void superSam() {
                  superSam();
                  System.out.println("do something else");
                }

                @Override
                // BUG: Diagnostic contains:
                default void otherSuperSam() {
                  subSam();
                  System.out.println("do something else");
                }
              }

              @FunctionalInterface
              interface ValueReturningSuperFI {
                String superSam();
              }

              @FunctionalInterface
              interface ValueReturningSubFI extends ValueReturningSuperFI {
                String subSam();

                @Override
                // BUG: Diagnostic contains:
                default String superSam() {
                  System.out.println("do something else");
                  return subSam();
                }
              }

              @FunctionalInterface
              public interface ValueReturningSubFI2 extends ValueReturningSuperFI {

                String subSam();

                @Override
                // BUG: Diagnostic contains:
                default String superSam() {
                  subSam();
                  return null;
                }
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "FunctionalInterfaceMethodChangedNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import java.util.concurrent.Callable;

            public class FunctionalInterfaceMethodChangedNegativeCases {
              @FunctionalInterface
              interface SuperFI {
                void superSam();
              }

              @FunctionalInterface
              interface OtherSuperFI {
                void otherSuperSam();
              }

              @FunctionalInterface
              interface SubFI extends SuperFI {
                void subSam();

                @Override
                default void superSam() {
                  subSam();
                }
              }

              @FunctionalInterface
              interface MultipleInheritanceSubFI extends SuperFI, OtherSuperFI {
                void subSam();

                @Override
                default void superSam() {
                  subSam();
                }

                @Override
                default void otherSuperSam() {
                  subSam();
                }
              }

              @FunctionalInterface
              interface ValueReturningSuperFI {
                String superSam();
              }

              @FunctionalInterface
              interface ValueReturningSubFI extends ValueReturningSuperFI {
                String subSam();

                @Override
                default String superSam() {
                  return subSam();
                }
              }

              // Regression test for b/68075767
              @FunctionalInterface
              public interface VoidCallable extends Callable<Void> {

                void voidCall() throws Exception;

                @Override
                default Void call() throws Exception {
                  voidCall();
                  return null;
                }
              }
            }\
            """)
        .doTest();
  }
}
