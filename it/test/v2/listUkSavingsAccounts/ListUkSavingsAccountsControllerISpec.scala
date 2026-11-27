/*
 * Copyright 2026 HM Revenue & Customs
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

package v2.listUkSavingsAccounts

import api.models.errors.*
import api.services.*
import api.support.IntegrationBaseSpec
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.*

class ListUkSavingsAccountsControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String                     = "AA123456A"
    private val savingsAccountId: String = "SAVKB2UVwUTBQGJ"

    val downstreamResponse: JsValue = Json.parse(
      """
        |{
        |  "bbsi": [
        |    {
        |      "incomeSourceId": "000000000000001",
        |      "incomeSourceName": "Bank Account 1",
        |      "startDate": "2025-04-06",
        |      "endDate": "2026-04-05"
        |    },
        |    {
        |      "incomeSourceId": "000000000000002",
        |      "incomeSourceName": "Bank Account 2",
        |      "startDate": "2025-04-06",
        |      "endDate": "2026-04-05"
        |    },
        |    {
        |      "incomeSourceId": "000000000000003",
        |      "incomeSourceName": "Bank Account 3",
        |      "startDate": "2025-04-06",
        |      "endDate": "2026-04-05"
        |    }
        |  ]
        |}
      """.stripMargin
    )

    val downstreamResponseWithDuplicates: JsValue = Json.parse(
      """
        |{
        |  "bbsi": [
        |    {
        |      "incomeSourceId": "000000000000001",
        |      "incomeSourceName": "Bank Account 1",
        |      "startDate": "2027-04-06",
        |      "endDate": "2028-04-05"
        |    },
        |    {
        |      "incomeSourceId": "000000000000001",
        |      "incomeSourceName": "Bank Account 1",
        |      "startDate": "2025-04-06",
        |      "endDate": "2026-04-05"
        |    },
        |    {
        |      "incomeSourceId": "000000000000002",
        |      "incomeSourceName": "Bank Account 2",
        |      "startDate": "2025-04-06",
        |      "endDate": "2026-04-05"
        |    },
        |    {
        |      "incomeSourceId": "000000000000003",
        |      "incomeSourceName": "Bank Account 3",
        |      "startDate": "2025-04-06",
        |      "endDate": "2026-04-05"
        |    }
        |  ]
        |}
      """.stripMargin
    )

    val mtdResponse: JsValue = Json.parse(
      """
        |{
        |  "savingsAccounts": [
        |    {
        |      "savingsAccountId": "000000000000001",
        |      "accountName": "Bank Account 1"
        |    },
        |    {
        |      "savingsAccountId": "000000000000002",
        |      "accountName": "Bank Account 2"
        |    },
        |    {
        |      "savingsAccountId": "000000000000003",
        |      "accountName": "Bank Account 3"
        |    }
        |  ]
        |}
      """.stripMargin
    )

    val mtdQueryParams: Seq[(String, String)] = Seq("savingsAccountId" -> savingsAccountId)

    val downstreamUri: String = s"/itsd/income-sources/v2/$nino"

    val downstreamQueryParams: Map[String, String] = Map("incomeSourceId" -> savingsAccountId)

    def setupStubs(): Unit = ()

    def request: WSRequest = {
      AuthStub.authorised()
      MtdIdLookupStub.ninoFound(nino)
      setupStubs()
      buildRequest(s"/uk-accounts/$nino")
        .addQueryStringParameters(mtdQueryParams*)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.2.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

    def errorBody(code: String): String =
      s"""
        |[
        |  {
        |    "errorCode": "$code",
        |    "errorDescription": "error description"
        |  }
        |]
      """.stripMargin

  }

  "Calling the 'List All UK Savings Accounts' endpoint" should {
    "return a 200 status code" when {
      "any valid request is made with query parameter savingsAccountId" in new Test {
        override def setupStubs(): Unit = DownstreamStub.onSuccess(
          method = DownstreamStub.GET,
          uri = downstreamUri,
          queryParams = downstreamQueryParams,
          status = OK,
          body = downstreamResponse
        )

        val response: WSResponse = await(request.get())

        response.status shouldBe OK
        response.json shouldBe mtdResponse
        response.header("Content-Type") shouldBe Some("application/json")
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }

      "any valid request is made without query parameter savingsAccountId" in new Test {
        override val mtdQueryParams: Seq[(String, String)]      = Seq.empty
        override val downstreamQueryParams: Map[String, String] = Map.empty

        override def setupStubs(): Unit = DownstreamStub.onSuccess(
          method = DownstreamStub.GET,
          uri = downstreamUri,
          queryParams = downstreamQueryParams,
          status = OK,
          body = downstreamResponse
        )

        val response: WSResponse = await(request.get())

        response.status shouldBe OK
        response.json shouldBe mtdResponse
        response.header("Content-Type") shouldBe Some("application/json")
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }

      "duplicate savings accounts are returned from downstream" in new Test {
        override def setupStubs(): Unit = DownstreamStub.onSuccess(
          method = DownstreamStub.GET,
          uri = downstreamUri,
          queryParams = downstreamQueryParams,
          status = OK,
          body = downstreamResponseWithDuplicates
        )

        val response: WSResponse = await(request.get())

        response.status shouldBe OK
        response.json shouldBe mtdResponse
        response.header("Content-Type") shouldBe Some("application/json")
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {
      def validationErrorTest(requestNino: String, requestSavingsAccountId: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"validation fails with ${expectedBody.code} error" in new Test {
          override val nino: String                          = requestNino
          override val mtdQueryParams: Seq[(String, String)] = Seq("savingsAccountId" -> requestSavingsAccountId)

          val response: WSResponse = await(request.get())

          response.status shouldBe expectedStatus
          response.json shouldBe expectedBody.asJson
          response.header("Content-Type") shouldBe Some("application/json")
        }
      }

      val validationInput: Seq[(String, String, Int, MtdError)] = Seq(
        ("AA1123A", "SAVKB2UVwUTBQGJ", BAD_REQUEST, NinoFormatError),
        ("AA123456A", "SAVKB2UVwUTBQG", BAD_REQUEST, SavingsAccountIdFormatError)
      )

      validationInput.foreach(validationErrorTest.tupled)

      def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
        s"downstream returns a code $downstreamCode error and status $downstreamStatus" in new Test {
          override def setupStubs(): Unit = DownstreamStub.onError(
            method = DownstreamStub.GET,
            uri = downstreamUri,
            queryParams = downstreamQueryParams,
            errorStatus = downstreamStatus,
            errorBody = errorBody(downstreamCode)
          )

          val response: WSResponse = await(request.get())

          response.status shouldBe expectedStatus
          response.json shouldBe expectedBody.asJson
          response.header("Content-Type") shouldBe Some("application/json")
          response.header("X-CorrelationId").nonEmpty shouldBe true
        }
      }

      val serviceInput: Seq[(Int, String, Int, MtdError)] = Seq(
        (BAD_REQUEST, "1215", BAD_REQUEST, NinoFormatError),
        (BAD_REQUEST, "1117", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "1122", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "1007", BAD_REQUEST, SavingsAccountIdFormatError),
        (BAD_REQUEST, "1229", INTERNAL_SERVER_ERROR, InternalError),
        (NOT_FOUND, "5010", NOT_FOUND, NotFoundError)
      )

      serviceInput.foreach(serviceErrorTest.tupled)
    }
  }

}
