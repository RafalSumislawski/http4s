/*
 * Copyright 2014 http4s.org
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

package fs2

import cats.effect.IO
import fs2.internal.FreeC.Output

import java.lang.reflect.Field

object Fs2ChunkExtractor {

  def tryExtractingChunk[F[_]](s: Stream[F, Byte]): Option[Chunk[Byte]] =
    freeInStream.flatMap(field =>
      field.get(s) match {
        case Output(values) => Some(values.asInstanceOf[Chunk[Byte]])
        case _ => None
      }
    )

  private val freeInStream: Option[Field] = {
    val field = classOf[Stream[IO, _]].getDeclaredFields.headOption
    field.foreach(_.setAccessible(true))
    field
  }

}
