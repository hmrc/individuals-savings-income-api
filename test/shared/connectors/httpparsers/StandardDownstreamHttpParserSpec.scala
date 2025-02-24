/*
 * Copyright 2023 HM Revenue & Customs
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

package shared.connectors.httpparsers

import play.api.http.Status._
import play.api.libs.json.{JsValue, Json, Reads}
import shared.connectors.DownstreamOutcome
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.utils.UnitSpec
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

class StandardDownstreamHttpParserSpec extends UnitSpec {

  // WLOG if Reads tested elsewhere
  case class SomeDataObject(data: String)

  object SomeDataObject {
    implicit val reads: Reads[SomeDataObject] = Json.reads
  }

  val method = "POST"
  val url    = "test-url"

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  import shared.connectors.httpparsers.StandardDownstreamHttpParser._

  val httpReads: HttpReads[DownstreamOutcome[Unit]] = implicitly

  val data                  = "someData"
  val expectedJson: JsValue = Json.obj("data" -> data)

  val downstreamResponseData: SomeDataObject              = SomeDataObject(data)
  val downstreamResponse: ResponseWrapper[SomeDataObject] = ResponseWrapper(correlationId, downstreamResponseData)

  "The generic HTTP parser" when {
    "no status code is specified" should {
      val httpReads: HttpReads[DownstreamOutcome[SomeDataObject]] = implicitly

      "return a Right downstream response containing the data object" in {
        val httpResponse = HttpResponse(OK, expectedJson, Map("CorrelationId" -> List(correlationId)))

        httpReads.read(method, url, httpResponse) shouldBe Right(downstreamResponse)
      }

      "return an outbound error if a data object cannot be read from the response json" in {
        val badFieldTypeJson: JsValue = Json.obj("incomeSourceId" -> 1234, "incomeSourceName" -> 1234)
        val httpResponse              = HttpResponse(OK, badFieldTypeJson.toString(), Map("CorrelationId" -> List(correlationId)))
        val expected                  = ResponseWrapper(correlationId, OutboundError(InternalError))

        httpReads.read(method, url, httpResponse) shouldBe Left(expected)
      }
    }

    "a success code is specified" should {
      "use that status code for success" in {
        implicit val successCode: SuccessCode                       = SuccessCode(PARTIAL_CONTENT)
        val httpReads: HttpReads[DownstreamOutcome[SomeDataObject]] = implicitly

        val httpResponse = HttpResponse(PARTIAL_CONTENT, expectedJson.toString(), Map("CorrelationId" -> List(correlationId)))

        httpReads.read(method, url, httpResponse) shouldBe Right(downstreamResponse)
      }
    }

    handleErrorsCorrectly(httpReads)
    handleInternalErrorsCorrectly(httpReads)
    handleUnexpectedResponse(httpReads)
    handleBvrsCorrectly(httpReads)
    handleHipErrorsCorrectly(httpReads)
  }

  "The generic HTTP parser for empty response with no header extraction" when {
    "no status code is specified" should {
      val httpReads: HttpReads[DownstreamOutcome[Unit]] = implicitly

      "receiving a 204 response" should {
        "return a Right downstream Response with the correct correlationId and no responseData" in {
          val httpResponse = HttpResponse(NO_CONTENT, "", headers = Map("CorrelationId" -> List(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Right(ResponseWrapper(correlationId, ()))
        }
      }
    }

    "a success code is specified" should {
      implicit val successCode: SuccessCode             = SuccessCode(PARTIAL_CONTENT)
      val httpReads: HttpReads[DownstreamOutcome[Unit]] = implicitly

      "use that status code for success" in {
        val httpResponse = HttpResponse(PARTIAL_CONTENT, "", headers = Map("CorrelationId" -> List(correlationId)))

        httpReads.read(method, url, httpResponse) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }

    handleErrorsCorrectly(httpReads)
    handleInternalErrorsCorrectly(httpReads)
    handleUnexpectedResponse(httpReads)
    handleBvrsCorrectly(httpReads)
    handleHipErrorsCorrectly(httpReads)
  }

  "The generic HTTP parser for empty responses with header extraction" when {
    val headerName  = "someHeader"
    val headerValue = "someHeaderValue"

    case class SomeCaseClass(value: String)

    "using the default success code (201 Created)" should {
      val httpReadsWithHeader: HttpReads[DownstreamOutcome[SomeCaseClass]] =
        readsEmptyWithHeader[SomeCaseClass](headerName, SomeCaseClass)

      "return a Right downstream response with SomeCaseClass when the expected header is present" in {
        val httpResponse = HttpResponse(CREATED, "", headers = Map(headerName -> List(headerValue), "CorrelationId" -> List(correlationId)))

        httpReadsWithHeader.read(method, url, httpResponse) shouldBe Right(ResponseWrapper(correlationId, SomeCaseClass(headerValue)))
      }

      "return an outbound error when the expected header is absent" in {
        val httpResponse = HttpResponse(CREATED, "", headers = Map("CorrelationId" -> List(correlationId)))

        httpReadsWithHeader.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
      }
    }

    "using a custom success code (202 Accepted)" should {
      implicit val successCode: SuccessCode = SuccessCode(ACCEPTED)

      val httpReadsWithHeader: HttpReads[DownstreamOutcome[SomeCaseClass]] =
        readsEmptyWithHeader[SomeCaseClass](headerName, SomeCaseClass)

      "return a Right downstream response with SomeCaseClass when the expected header is present" in {
        val httpResponse = HttpResponse(ACCEPTED, "", headers = Map(headerName -> List(headerValue), "CorrelationId" -> List(correlationId)))

        httpReadsWithHeader.read(method, url, httpResponse) shouldBe Right(ResponseWrapper(correlationId, SomeCaseClass(headerValue)))
      }

      "return an outbound error when the expected header is absent" in {
        val httpResponse = HttpResponse(ACCEPTED, "", headers = Map("CorrelationId" -> List(correlationId)))

        httpReadsWithHeader.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
      }
    }

    handleErrorsCorrectly(httpReads)
    handleInternalErrorsCorrectly(httpReads)
    handleUnexpectedResponse(httpReads)
    handleBvrsCorrectly(httpReads)
    handleHipErrorsCorrectly(httpReads)
  }

  val singleErrorJson: JsValue = Json.parse(
    """
      |{
      |   "code": "CODE",
      |   "reason": "MESSAGE"
      |}
    """.stripMargin
  )

  val multipleErrorsJson: JsValue = Json.parse(
    """
      |{
      |   "failures": [
      |       {
      |           "code": "CODE 1",
      |           "reason": "MESSAGE 1"
      |       },
      |       {
      |           "code": "CODE 2",
      |           "reason": "MESSAGE 2"
      |       }
      |   ]
      |}
    """.stripMargin
  )

  val malformedErrorJson: JsValue = Json.parse(
    """
      |{
      |   "coed": "CODE",
      |   "resaon": "MESSAGE"
      |}
    """.stripMargin
  )

  private def handleErrorsCorrectly[A](httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    List(BAD_REQUEST, NOT_FOUND, FORBIDDEN, CONFLICT, GONE, UNPROCESSABLE_ENTITY).foreach(responseCode =>
      s"receiving a $responseCode response" should {
        "be able to parse a single error" in {
          val httpResponse = HttpResponse(responseCode, singleErrorJson.toString(), Map("CorrelationId" -> List(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Left(
            ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode("CODE"))))
        }

        "be able to parse multiple errors" in {
          val httpResponse = HttpResponse(responseCode, multipleErrorsJson.toString(), Map("CorrelationId" -> List(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe {
            Left(ResponseWrapper(correlationId, DownstreamErrors(List(DownstreamErrorCode("CODE 1"), DownstreamErrorCode("CODE 2")))))
          }
        }

        "return an outbound error when the error returned doesn't match the Error model" in {
          val httpResponse = HttpResponse(responseCode, malformedErrorJson.toString(), Map("CorrelationId" -> List(correlationId)))

          val result = httpReads.read(method, url, httpResponse)
          result shouldBe Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
        }
      })

  private def handleInternalErrorsCorrectly[A](httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    List(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach(responseCode =>
      s"receiving a $responseCode response" should {
        "return an outbound error when the error returned matches the Error model" in {
          val httpResponse = HttpResponse(responseCode, singleErrorJson.toString, Map("CorrelationId" -> List(correlationId)))

          val result = httpReads.read(method, url, httpResponse)
          result shouldBe Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
        }

        "return an outbound error when the error returned doesn't match the Error model" in {
          val httpResponse = HttpResponse(responseCode, malformedErrorJson, Map("CorrelationId" -> List(correlationId)))

          val result = httpReads.read(method, url, httpResponse)
          result shouldBe Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
        }
      })

  private def handleUnexpectedResponse[A](httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    "receiving an unexpected response" should {
      val responseCode = 499
      "return an outbound error when the error returned matches the Error model" in {
        val httpResponse = HttpResponse(responseCode, singleErrorJson, Map("CorrelationId" -> List(correlationId)))

        val result = httpReads.read(method, url, httpResponse)
        result shouldBe Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
      }

      "return an outbound error when the error returned doesn't match the Error model" in {
        val httpResponse = HttpResponse(responseCode, malformedErrorJson, Map("CorrelationId" -> List(correlationId)))

        val result = httpReads.read(method, url, httpResponse)
        result shouldBe Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
      }
    }

  private def handleBvrsCorrectly[A](httpReads: HttpReads[DownstreamOutcome[A]]): Unit = {

    val singleBvrJson = Json.parse(
      """
        |{
        |   "bvrfailureResponseElement": {
        |     "validationRuleFailures": [
        |       {
        |         "id": "BVR1"
        |       },
        |       {
        |         "id": "BVR2"
        |       }
        |     ]
        |   }
        |}
      """.stripMargin
    )

    "receiving a response with BVR errors" should {
      "return an outbound BUSINESS_ERROR error containing the BVR ids" in {
        val httpResponse = HttpResponse(BAD_REQUEST, singleBvrJson, Map("CorrelationId" -> List(correlationId)))
        val result       = httpReads.read(method, url, httpResponse)

        result shouldBe Left(
          ResponseWrapper(
            correlationId,
            OutboundError(
              BVRError,
              Some(
                List(
                  MtdError("BVR1", "", BAD_REQUEST),
                  MtdError("BVR2", "", BAD_REQUEST)
                )))
          )
        )
      }
    }
  }

  val singleHipErrorJson: JsValue = Json.parse(
    """
      |{
      |    "errorCode": "1011",
      |    "errorDescription": "Some error occurred",
      |    "validationRuleFailures": [
      |        {
      |            "id": "string",
      |            "type": "ERR",
      |            "text": "string"
      |        }
      |    ]
      |}
    """.stripMargin
  )

  val multipleHipErrorsJson: JsValue = Json.parse(
    """
      |{
      |    "origin": "HIP",
      |    "response": {
      |        "failures": [
      |            {
      |                "type": "ERR1",
      |                "reason": "Error 1 description"
      |            },
      |            {
      |                "type": "ERR2",
      |                "reason": "Error 2 description"
      |            }
      |        ]
      |    }
      |}
    """.stripMargin
  )

  private def handleHipErrorsCorrectly[A](httpReads: HttpReads[DownstreamOutcome[A]]): Unit = {

    "receiving a 400 response with multiple HIP errors" should {
      "return a Left ResponseWrapper containing the extracted error types" in {
        val httpResponse = HttpResponse(BAD_REQUEST, multipleHipErrorsJson, Map("CorrelationId" -> List(correlationId)))

        httpReads.read(method, url, httpResponse) shouldBe Left(
          ResponseWrapper(correlationId, DownstreamErrors(List(DownstreamErrorCode("ERR1"), DownstreamErrorCode("ERR2"))))
        )
      }
    }

    "receiving a 422 response with single HIP error" should {
      "return a Left ResponseWrapper containing the extracted error code" in {
        val httpResponse = HttpResponse(UNPROCESSABLE_ENTITY, singleHipErrorJson, Map("CorrelationId" -> List(correlationId)))

        httpReads.read(method, url, httpResponse) shouldBe Left(
          ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode("1011")))
        )
      }
    }
  }

}
