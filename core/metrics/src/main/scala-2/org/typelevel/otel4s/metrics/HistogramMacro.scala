/*
 * Copyright 2022 Typelevel
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

package org.typelevel.otel4s
package metrics

import cats.effect.kernel.Resource

import scala.concurrent.duration.TimeUnit

private[otel4s] trait HistogramMacro[F[_], A] {
  def backend: Histogram.Backend[F, A]

  /** Records a value with a set of attributes.
    *
    * @param value
    *   the value to record
    *
    * @param attributes
    *   the set of attributes to associate with the value
    */
  def record(value: A, attributes: Attribute[_]*): F[Unit] =
    macro HistogramMacro.record[A]

  /** Records duration of the given effect.
    *
    * @example
    *   {{{
    * val histogram: Histogram[F] = ???
    * val attributeKey = AttributeKey.string("query_name")
    *
    * def findUser(name: String) =
    *   histogram.recordDuration(TimeUnit.MILLISECONDS, Attribute(attributeKey, "find_user")).use { _ =>
    *     db.findUser(name)
    *    }
    *   }}}
    *
    * @param timeUnit
    *   the time unit of the duration measurement
    *
    * @param attributes
    *   the set of attributes to associate with the value
    */
  def recordDuration(
      timeUnit: TimeUnit,
      attributes: Attribute[_]*
  ): Resource[F, Unit] =
    macro HistogramMacro.recordDuration

}

object HistogramMacro {
  import scala.reflect.macros.blackbox

  def record[A](c: blackbox.Context)(
      value: c.Expr[A],
      attributes: c.Expr[Attribute[_]]*
  ): c.universe.Tree = {
    import c.universe._
    val backend = q"${c.prefix}.backend"
    val meta = q"$backend.meta"

    q"if ($meta.isEnabled) $backend.record($value, ..$attributes) else $meta.unit"
  }

  def recordDuration(c: blackbox.Context)(
      timeUnit: c.Expr[TimeUnit],
      attributes: c.Expr[Attribute[_]]*
  ): c.universe.Tree = {
    import c.universe._
    val backend = q"${c.prefix}.backend"
    val meta = q"$backend.meta"

    q"if ($meta.isEnabled) $backend.recordDuration($timeUnit, ..$attributes) else $meta.resourceUnit"
  }

}
