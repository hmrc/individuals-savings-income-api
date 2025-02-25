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

package v1.addUkSavingsAccount.def1

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.errors.{AccountNameFormatError, RuleDuplicateAccountNameError, RuleMaximumSavingsAccountsLimitError}
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.{ACCEPT, AUTHORIZATION}
import shared.models.errors._
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class Def1_AddUkSavingsAccountControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String             = "AA123456A"
    val savingsAccountId: String = "SAVKB2UVwUTBQGJ"
    val taxYear: String          = "2020-21"

    val requestBodyJson: JsValue = Json.parse(
      """
        |{
        |   "accountName": "Shares savings account"
        |}
      """.stripMargin
    )

    val responseJson: JsValue = Json.parse(
      s"""
        |{
        |   "savingsAccountId": "$savingsAccountId"
        |}
      """.stripMargin
    )

    private def uri: String = s"/uk-accounts/$nino"

    def downstreamUri: String = s"/itsd/income-sources/$nino"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.1.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

  }

  "calling the 'add uk savings account' endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.POST, downstreamUri, CREATED, Map("IncomeSourceId" -> savingsAccountId))
        }

        val response: WSResponse = await(request().post(requestBodyJson))
        response.status shouldBe OK
        response.body[JsValue] shouldBe responseJson
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return error according to spec" when {

      val validRequestJson: JsValue = Json.parse(
        """
          |{
          |   "accountName": "Shares savings account"
          |}
        """.stripMargin
      )

      val emptyRequestJson: JsValue = JsObject.empty

      val nonValidRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |   "accountName": "Shares savings account!"
          |}
        """.stripMargin
      )

      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedBody: MtdError,
                                scenario: Option[String]): Unit = {
          s"validation fails with ${expectedBody.code} error ${scenario.getOrElse("")}" in new Test {

            override val nino: String             = requestNino
            override val taxYear: String          = requestTaxYear
            override val requestBodyJson: JsValue = requestBody

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request().post(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val accountNameError: MtdError = AccountNameFormatError.copy(
          paths = Some(List("/accountName"))
        )
        val input = List(
          ("AA1123A", "2019-20", validRequestJson, BAD_REQUEST, NinoFormatError, None),
          ("AA123456A", "2019-20", emptyRequestJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError, None),
          ("AA123456A", "2019-20", nonValidRequestBodyJson, BAD_REQUEST, accountNameError, None)
        )
        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "downstream service error" when {
        def serviceCodeErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns a code $downstreamCode error and status $downstreamStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.POST, downstreamUri, downstreamStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request().post(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        def serviceStatusErrorTest(downstreamStatus: Int, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns a status $downstreamStatus error" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.POST, downstreamUri, downstreamStatus)
            }

            val response: WSResponse = await(request().post(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        def errorBody(code: String): String =
          s"""
            |{
            |   "errorCode": "$code",
            |   "errorDescription": "downstream message",
            |    "validationRuleFailures": [
            |        {
            |            "id": "string",
            |            "type": "ERR",
            |            "text": "string"
            |        }
            |    ]
            |}
          """.stripMargin

        val desErrors = List(
          (BAD_REQUEST, "INVALID_IDVALUE", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_IDTYPE", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError),
          (CONFLICT, "MAX_ACCOUNTS_REACHED", BAD_REQUEST, RuleMaximumSavingsAccountsLimitError),
          (CONFLICT, "ALREADY_EXISTS", BAD_REQUEST, RuleDuplicateAccountNameError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
        )

        val hipCodeErrors = List((UNPROCESSABLE_ENTITY, "1011", BAD_REQUEST, RuleMaximumSavingsAccountsLimitError))

        val hipStatusErrors = List((CONFLICT, BAD_REQUEST, RuleDuplicateAccountNameError))

        (desErrors ++ hipCodeErrors).foreach(args => (serviceCodeErrorTest _).tupled(args))

        hipStatusErrors.foreach(args => (serviceStatusErrorTest _).tupled(args))
      }
    }
  }

}
