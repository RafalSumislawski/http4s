/*
 * Copyright 2015 http4s.org
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

package org.http4s.bench

import cats.effect.{ContextShift, IO, Timer}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.{Router, Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{HttpRoutes, Method, Request, Uri}
import org.openjdk.jmh.annotations._

import java.util.concurrent.{ForkJoinPool, TimeUnit}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import cats.implicits._
import cats.effect.implicits._
import org.slf4j.LoggerFactory
import org.http4s.bench.BlazeBench.{BlazeBenchCounters, BlazeBenchState};

// sbt "bench/jmh:run -i 10 -wi 10 -f 2 -t 1 org.http4s.bench.BlazeBench"
// or sbt "bench/jmh:run -i 2 -wi 2 -f 5 -t 1 org.http4s.bench.BlazeBench" if the goal is to reproduce #2277
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class BlazeBench {

  LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    .asInstanceOf[ch.qos.logback.classic.Logger]
    .setLevel(ch.qos.logback.classic.Level.WARN)

  @Benchmark
  def runRequests(state: BlazeBenchState, counters: BlazeBenchCounters): Vector[Any] = {
    import state._
    val requests = 10 * 1000
    val parallelism = 1024
    val result = Vector.fill(requests)(())
      .parTraverseN(parallelism)(_ =>
          client.expect[String](Request[IO](method = Method.POST, uri = BlazeBench.uri).withEntity("world"))
            .handleErrorWith(t => IO {
              counters.synchronized(counters.failures += 1)
              t.printStackTrace()
            })
      ).unsafeRunSync()
    counters.requests += requests
    result
  }
}

object BlazeBench {

  val uri = Uri.unsafeFromString(s"http://127.0.0.1:12345")

  @State(Scope.Thread)
  @AuxCounters(AuxCounters.Type.OPERATIONS)
  class BlazeBenchCounters {
    var requests = 0
    var failures = 0 // the synchronisation of access to this variable is wrong

    @Setup
    def setUp(): Unit = {
      requests = 0
      failures = 0
    }
  }

  @State(Scope.Benchmark)
  class BlazeBenchState {

    println("Constructing state")

    val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool())

    implicit val contextShift: ContextShift[IO] =
      IO.contextShift(executionContext)

    implicit val timer: Timer[IO] =
      IO.timer(executionContext)

    val (server: Server[IO], releaseServer) = BlazeServerBuilder[IO](executionContext)
      .withMaxConnections(16 * 1024)
      .bindLocal(12345)
      .withHttpApp(Router("/" -> HttpRoutes.of[IO] {
        case req@POST -> Root => req.as[String].flatMap(input => Ok(s"World $input!"))
      }).orNotFound)
      .allocated.unsafeRunSync()

    val (client: Client[IO], releaseClient) = BlazeClientBuilder[IO](executionContext)
      .withMaxTotalConnections(16 * 1024)
      .withMaxConnectionsPerRequestKey(_ => 16 * 1024)
      .allocated.unsafeRunSync()

    @TearDown(Level.Trial) def doTearDown(): Unit = {
      System.out.println("Do TearDown")
      releaseServer.unsafeRunSync()
      releaseClient.unsafeRunSync()
    }
  }

  def main(args: Array[String]): Unit = {
    val benchState = new BlazeBenchState()
    benchState.client.expect[String](Request[IO](method = Method.POST, uri = uri).withEntity("world"))
      .handleErrorWith(t => IO(t.printStackTrace()))
      .attemptTap(result => IO(println(result)))
      .unsafeRunSync()
    System.exit(0)
  }
}
