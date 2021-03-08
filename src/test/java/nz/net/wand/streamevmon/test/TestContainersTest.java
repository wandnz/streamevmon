/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.test;

import org.scalatest.TagAnnotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a test suite and its children to be excluded from the test run if the
 * noDocker:test target is executed. Note that for this annotation to be
 * inherited to all children, each child in the inheritance chain must be a
 * class (or abstract class), <strong>not</strong> a trait!
 *
 * @see <a href="https://www.scalatest.org/scaladoc/3.1.1/org/scalatest/TagAnnotation.html">TagAnnotation</a>
 * documentation for evidence that this must be implemented in Java so that
 * it is visible at runtime.
 */
@TagAnnotation
@Retention(RUNTIME)
@Target({METHOD, TYPE})
@Inherited
public @interface TestContainersTest {
}
