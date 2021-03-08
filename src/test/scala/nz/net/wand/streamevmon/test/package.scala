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

package nz.net.wand.streamevmon

/** This package contains traits and objects used in testing. Most notable are
  * TestBase, which all tests should extend, and SeedData, which contains a
  * large amount of useful example objects of various types.
  *
  * `PostgresContainerSpec` and `InfluxContainerSpec` include some pre-baked
  * setup for running those types of database. You should use these instead of
  * creating your own if you need them.
  *
  * If you need to use a different type of TestContainer, you should inherit our
  * `TaggedForXTestContainer` classes instead of the regular `ForXTestContainer`
  * povided by testcontainers-scala. This allows us to skip tests that rely on
  * Docker in environments where it is not available. Since these must be
  * abstract classes rather than traits, they must be first in the list of
  * extended classes.
  *
  * `HarnessingTest` provides an interface for using the Flink test harness,
  * which allows you to test checkpointing and other things on ProcessFunctions
  * without spinning up a full Flink environment.
  *
  * Note that there is a member of this package (TestContainersTest) written in
  * Java, since a scalatest tag annotation must be in Java to be viewable at
  * runtime.
  */
package object test {}
