/*
 * Copyright 2008 The Closure Compiler Authors.
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

package com.google.javascript.jscomp.deps;

import com.google.common.collect.ImmutableList;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.PrintStreamErrorManager;

import junit.framework.TestCase;

import java.util.Collections;

/**
 * Tests for {@link JsFileParser}.
 *
 * @author agrieve@google.com (Andrew Grieve)
 */
public class JsFileParserTest extends TestCase {

  JsFileParser parser;
  private ErrorManager errorManager;

  private static final String SRC_PATH = "a";
  private static final String CLOSURE_PATH = "b";

  @Override
  public void setUp() {
    errorManager = new PrintStreamErrorManager(System.err);
    parser = new JsFileParser(errorManager);
    parser.setShortcutMode(true);
  }

  /**
   * Tests:
   *  -Parsing of comments,
   *  -Parsing of different styles of quotes,
   *  -Correct recording of what was parsed.
   */
  public void testParseFile() {
    final String CONTENTS = "/*"
      + "goog.provide('no1');*//*\n"
      + "goog.provide('no2');\n"
      + "*/goog.provide('yes1');\n"
      + "/* blah */goog.provide(\"yes2\")/* blah*/\n"
      + "goog.require('yes3'); // goog.provide('no3');\n"
      + "// goog.provide('no4');\n"
      + "goog.require(\"bar.data.SuperstarAddStarThreadActionRequestDelegate\"); "
      + "//no new line at eof";

    DependencyInfo EXPECTED = new SimpleDependencyInfo(CLOSURE_PATH, SRC_PATH,
        ImmutableList.of("yes1", "yes2"),
        ImmutableList.of("yes3", "bar.data.SuperstarAddStarThreadActionRequestDelegate"));

    DependencyInfo result = parser.parseFile(SRC_PATH, CLOSURE_PATH, CONTENTS);

    assertEquals(EXPECTED, result);
    assertEquals(0, errorManager.getErrorCount());
    assertEquals(0, errorManager.getWarningCount());
  }

  public void testMultiplePerLine() {
    final String CONTENTS = "goog.provide('yes1');goog.provide('yes2');/*"
        + "goog.provide('no1');*/goog.provide('yes3');//goog.provide('no2');";

    DependencyInfo EXPECTED = new SimpleDependencyInfo(CLOSURE_PATH, SRC_PATH,
        ImmutableList.of("yes1", "yes2", "yes3"), Collections.<String>emptyList());

    DependencyInfo result = parser.parseFile(SRC_PATH, CLOSURE_PATH, CONTENTS);

    assertEquals(EXPECTED, result);
    assertEquals(0, errorManager.getErrorCount());
    assertEquals(0, errorManager.getWarningCount());
  }

  public void testShortcutMode1() {
    // For efficiency reasons, we stop reading after the ctor.
    final String CONTENTS = " // hi ! \n /* this is a comment */ "
        + "goog.provide('yes1');\n /* and another comment */ \n"
        + "goog.provide('yes2'); // include this\n"
        + "function foo() {}\n"
        + "goog.provide('no1');";

    DependencyInfo EXPECTED = new SimpleDependencyInfo(CLOSURE_PATH, SRC_PATH,
        ImmutableList.of("yes1", "yes2"), Collections.<String>emptyList());
    DependencyInfo result = parser.parseFile(SRC_PATH, CLOSURE_PATH, CONTENTS);

    assertEquals(EXPECTED, result);
    assertEquals(0, errorManager.getErrorCount());
    assertEquals(0, errorManager.getWarningCount());
  }

  public void testShortcutMode2() {
    final String CONTENTS = "/** goog.provide('no1'); \n" +
        " * goog.provide('no2');\n */\n"
        + "goog.provide('yes1');\n";

    DependencyInfo EXPECTED = new SimpleDependencyInfo(CLOSURE_PATH, SRC_PATH,
        ImmutableList.of("yes1"), Collections.<String>emptyList());
    DependencyInfo result = parser.parseFile(SRC_PATH, CLOSURE_PATH, CONTENTS);

    assertEquals(EXPECTED, result);
    assertEquals(0, errorManager.getErrorCount());
    assertEquals(0, errorManager.getWarningCount());
  }

  public void testShortcutMode3() {
    final String CONTENTS = "/**\n" +
        " * goog.provide('no1');\n */\n"
        + "goog.provide('yes1');\n";

    DependencyInfo EXPECTED = new SimpleDependencyInfo(CLOSURE_PATH, SRC_PATH,
        ImmutableList.of("yes1"), Collections.<String>emptyList());
    DependencyInfo result = parser.parseFile(SRC_PATH, CLOSURE_PATH, CONTENTS);

    assertEquals(EXPECTED, result);
    assertEquals(0, errorManager.getErrorCount());
    assertEquals(0, errorManager.getWarningCount());
  }
}
