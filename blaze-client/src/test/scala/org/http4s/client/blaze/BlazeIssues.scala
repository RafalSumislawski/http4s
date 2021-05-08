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

package org.http4s.client.blaze

import cats.effect.IO
import cats.implicits._
import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.http4s.client.jetty.JettyClient
import org.http4s.dsl.io._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{Http4sSuite, HttpRoutes, Uri}

import scala.concurrent.duration.DurationInt

class BlazeIssues extends Http4sSuite {

  //  test("failing to connect (wrong port)") {
  //    (
  //      BlazeServerBuilder[IO](munitExecutionContext)
  //        .withIdleTimeout(5.second)
  //        .bindLocal(12345)
  //        .withHttpApp(
  //          Router(
  //            "/" -> HttpRoutes.of[IO] {
  //              case GET -> Root / "slow" => IO.sleep(10.seconds) >> Ok("")
  //              case GET -> Root / "fast" => Ok("")
  //            }
  //          ).orNotFound
  //        )
  //        .resource,
  //      BlazeClientBuilder[IO](munitExecutionContext)
  //        .resource
  //      ).tupled.use { case (_, client) =>
  //      val u = Uri.fromString(s"http://127.0.0.1:12346").yolo
  //      client.expect[String](u).attempt.map(result => println(result))
  //    }.unsafeRunSync()
  //
  //    // This reports failure connecting. This is correct.
  //    // org.http4s.client.ConnectionFailure: Error connecting to http://127.0.0.1:12346 using address 127.0.0.1:12346 (unresolved: false)
  //  }

  //  test("failing to connect (server not accepting)") {
  //    (
  //      BlazeServerBuilder[IO](munitExecutionContext)
  //        .withIdleTimeout(5.second)
  //        .bindLocal(12347)
  //        .withMaxConnections(0)
  //        .withHttpApp(
  //          Router(
  //            "/" -> HttpRoutes.of[IO] {
  //              case GET -> Root / "slow" => IO.sleep(10.seconds) >> Ok("")
  //              case GET -> Root / "fast" => Ok("")
  //            }
  //          ).orNotFound
  //        )
  //        .resource,
  //      BlazeClientBuilder[IO](munitExecutionContext)
  //        .resource
  //      ).tupled.use { case (_, client) =>
  //      val u = Uri.fromString(s"http://127.0.0.1:12347").yolo
  //      client.expect[String](u).attempt.map(result => println(result))
  //    }.unsafeRunSync()
  //
  //    // This reports failure connecting. This is correct.
  //    // org.http4s.client.ConnectionFailure: Error connecting to http://127.0.0.1:12346 using address 127.0.0.1:12346 (unresolved: false)
  //  }

  //  test("loosing connection during processing of first request") {
  //    (
  //      BlazeServerBuilder[IO](munitExecutionContext)
  //        .withIdleTimeout(5.second)
  //        .bindLocal(12345)
  //        .withHttpApp(
  //          Router(
  //            "/" -> HttpRoutes.of[IO] {
  //              case GET -> Root / "slow" => IO.sleep(10.seconds) >> Ok("")
  //              case GET -> Root / "fast" => Ok("")
  //            }
  //          ).orNotFound
  //        )
  //        .resource,
  //      BlazeClientBuilder[IO](munitExecutionContext)
  //        .resource
  //      ).tupled.use { case (_, client) =>
  //      val u = Uri.fromString(s"http://127.0.0.1:12345/slow").yolo
  //      client.expect[String](u).attempt.map(result => println(result))
  //    }.unsafeRunSync()
  //
  //    // This says that it failed to connect, which is incorrect, because it connected successfully
  //    // java.net.ConnectException: Failed to connect to endpoint: http://127.0.0.1:12345
  //  }
  //
  //  test("loosing connection during processing of subsequent request") {
  //    (
  //      BlazeServerBuilder[IO](munitExecutionContext)
  //        .withIdleTimeout(5.second)
  //        .bindLocal(12345)
  //        .withHttpApp(
  //          Router(
  //            "/" -> HttpRoutes.of[IO] {
  //              case GET -> Root / "slow" => IO.sleep(10.seconds) >> Ok("")
  //              case GET -> Root / "fast" => IO.sleep(1.seconds) >> Ok("")
  //            }
  //          ).orNotFound
  //        )
  //        .resource,
  //      BlazeClientBuilder[IO](munitExecutionContext)
  //        .resource
  //      ).tupled.use { case (_, client) =>
  //      client.expect[String](Uri.fromString(s"http://127.0.0.1:12345/fast").yolo).attempt.map(result => println(result)) >>
  //        client.expect[String](Uri.fromString(s"http://127.0.0.1:12345/slow").yolo).attempt.map(result => println(result))
  //    }.unsafeRunSync()
  //
  //    // This sends two "slow" requests and waits 10 seconds.
  //    // java.net.ConnectException: Failed to connect to endpoint: http://127.0.0.1:12345
  //  }
  //
  //  test("loosing connection during processing of subsequent request - extreme case") {
  //    (
  //      BlazeServerBuilder[IO](munitExecutionContext)
  //        .withIdleTimeout(5.second)
  //        .bindLocal(12345)
  //        .withHttpApp(
  //          Router(
  //            "/" -> HttpRoutes.of[IO] {
  //              case GET -> Root / "slow" => IO.sleep(10.seconds) >> Ok("")
  //              case GET -> Root / "fast" => IO.sleep(1.seconds) >> Ok("")
  //            }
  //          ).orNotFound
  //        )
  //        .resource,
  //      BlazeClientBuilder[IO](munitExecutionContext)
  //        .withRequestTimeout(10.seconds)
  //        .resource
  //      ).tupled.use { case (_, client) =>
  //
  //        IO.sleep(3500.millis) >> client.expect[String](Uri.fromString(s"http://127.0.0.1:12345/fast").yolo).attempt.map(result => println(result)) >>
  //          IO.sleep(4000.millis) >> client.expect[String](Uri.fromString(s"http://127.0.0.1:12345/fast").yolo).attempt.map(result => println(result)) >>
  //          IO.sleep(4000.millis) >> client.expect[String](Uri.fromString(s"http://127.0.0.1:12345/fast").yolo).attempt.map(result => println(result)) >>
  //          IO.sleep(4000.millis) >> client.expect[String](Uri.fromString(s"http://127.0.0.1:12345/fast").yolo).attempt.map(result => println(result))
  //          .background.use(_ =>
  //            client.expect[String](Uri.fromString(s"http://127.0.0.1:12345/slow").yolo).attempt.map(result => println(result))
  //          )
  //    }.unsafeRunSync()
  //
  //    // This keeps resending "slow" requests while we feed it with connections created by "fast" requests
  //    // withRequestTimeout doesn't
  //    // java.net.ConnectException: Failed to connect to endpoint: http://127.0.0.1:12345
  //  }

  val server = BlazeServerBuilder[IO](munitExecutionContext)
    .withIdleTimeout(5.second)
    .bindLocal(12345)
    .withHttpApp(Router("/" -> HttpRoutes.of[IO] { case GET -> Root => IO.sleep(1.seconds) >> Ok("") }).orNotFound)
    .resource

  test("Blaze Client keeps half-closed connection after server sends FIN-ACK through an idle connection") {
    (
      server,
      BlazeClientBuilder[IO](munitExecutionContext).resource
    ).tupled.use { case (_, client) =>for {
      _ <- client.expect[String](Uri.fromString(s"http://127.0.0.1:12345").yolo).attemptTap(result => IO(println(result)))
      _ <- IO.sleep(6.seconds)
      _ <- client.expect[String](Uri.fromString(s"http://127.0.0.1:12345").yolo).attemptTap(result => IO(println(result)))
    } yield ()
    }.unsafeRunSync()
  }

  test("AsyncHttpClient closes connection after server sends FIN-ACK through an idle connection") {
    (
      server,
      AsyncHttpClient.resource[IO]()
    ).tupled.use { case (_, client) => for {
      _ <- client.expect[String](Uri.fromString(s"http://127.0.0.1:12345").yolo).attemptTap(result => IO(println(result)))
      _ <- IO.sleep(6.seconds)
      _ <- client.expect[String](Uri.fromString(s"http://127.0.0.1:12345").yolo).attemptTap(result => IO(println(result)))
    } yield ()
    }.unsafeRunSync()
  }

  test("EmberClient closes connection after server sends FIN-ACK through an idle connection") {
    (
      server,
      EmberClientBuilder.default[IO].build
      ).tupled.use { case (_, client) => for {
      _ <- client.expect[String](Uri.fromString(s"http://127.0.0.1:12345").yolo).attemptTap(result => IO(println(result)))
      _ <- IO.sleep(6.seconds)
      _ <- client.expect[String](Uri.fromString(s"http://127.0.0.1:12345").yolo).attemptTap(result => IO(println(result)))
    } yield ()
    }.unsafeRunSync()
  }

  test("JettyClient closes connection after server sends FIN-ACK through an idle connection") {
    (
      server,
      JettyClient.resource[IO]()
      ).tupled.use { case (_, client) => for {
      _ <- client.expect[String](Uri.fromString(s"http://127.0.0.1:12345").yolo).attemptTap(result => IO(println(result)))
      _ <- IO.sleep(6.seconds)
      _ <- client.expect[String](Uri.fromString(s"http://127.0.0.1:12345").yolo).attemptTap(result => IO(println(result)))
    } yield ()
    }.unsafeRunSync()
  }
}
