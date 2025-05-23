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

/**
 * Represents a pair of a formal parameter and an actual parameter. Pairs can correspond to those in
 * the original method invocation but can also represent potential alternatives and suggested
 * changes.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
record ParameterPair(Parameter formal, Parameter actual) {
  static ParameterPair create(Parameter formal, Parameter actual) {
    return new ParameterPair(formal, actual);
  }

  boolean isAlternativePairing() {
    return formal().index() != actual().index();
  }
}
