/*
 * Copyright 2022 HM Revenue & Customs
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

package api.connectors

import play.api.http.{ HeaderNames, MimeTypes, Status }
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait ConnectorSpec extends UnitSpec with Status with MimeTypes with HeaderNames {

  lazy val baseUrl = "http://test-BaseUrl"

  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  implicit val hc: HeaderCarrier     = HeaderCarrier()
  implicit val ec: ExecutionContext  = scala.concurrent.ExecutionContext.global

  val otherHeaders: Seq[(String, String)] = Seq(
    "Gov-Test-Scenario" -> "DEFAULT",
    "AnotherHeader"     -> "HeaderValue"
  )

  val dummyDesHeaderCarrierConfig: HeaderCarrier.Config =
    HeaderCarrier.Config(
      Seq("^not-test-BaseUrl?$".r),
      Seq.empty[String],
      Some("individuals-income-received-api")
    )

  val dummyIfsHeaderCarrierConfig: HeaderCarrier.Config =
    HeaderCarrier.Config(
      Seq("^not-test-BaseUrl?$".r),
      Seq.empty[String],
      Some("individuals-income-received-api")
    )

  val allowedDesHeaders: Seq[String] = Seq(
    "Accept",
    "Gov-Test-Scenario",
    "Content-Type",
    "Location",
    "X-Request-Timestamp",
    "X-Session-Id"
  )

  val allowedIfsHeaders: Seq[String] = Seq(
    "Accept",
    "Gov-Test-Scenario",
    "Content-Type",
    "Location",
    "X-Request-Timestamp",
    "X-Session-Id"
  )

  val requiredDesHeaders: Seq[(String, String)] = Seq(
    "Environment"   -> "des-environment",
    "Authorization" -> s"Bearer des-token",
    "CorrelationId" -> s"$correlationId"
  )

  val requiredIfsHeaders: Seq[(String, String)] = Seq(
    "Environment"   -> "ifs-environment",
    "Authorization" -> s"Bearer ifs-token",
    "CorrelationId" -> s"$correlationId"
  )

  val requiredRelease6Headers: Seq[(String, String)] = Seq(
    "Environment"   -> "release6-environment",
    "Authorization" -> s"Bearer release6-token",
    "CorrelationId" -> s"$correlationId"
  )

  val requiredApi1661Headers: Seq[(String, String)] = Seq(
    "Environment"   -> "api1661-environment",
    "Authorization" -> s"Bearer api1661-token",
    "CorrelationId" -> s"$correlationId"
  )
}