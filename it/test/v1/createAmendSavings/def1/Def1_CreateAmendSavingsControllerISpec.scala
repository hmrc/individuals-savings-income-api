/*
 * Copyright 2025 HM Revenue & Customs
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

package v1.createAmendSavings.def1

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.errors
import shared.models.errors._
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec

class Def1_CreateAmendSavingsControllerISpec extends IntegrationBaseSpec {

  "Calling the 'create amend savings' endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new NonTysTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
      }

      "any valid request is made for Tax Year Specific (TYS)" in new TysIfsTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT)
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
      }
    }

    "return a 400 with multiple errors" when {
      "all field value validations fail on the request body" in new NonTysTest {

        val allInvalidValueRequestBodyJson: JsValue = Json.parse(
          """
            |{
            |   "securities": {
            |      "taxTakenOff": 100.111,
            |      "grossAmount": -100.12,
            |      "netAmount": 999999999991.99
            |   },
            |   "foreignInterest": [
            |       {
            |          "amountBeforeTax": -200.11,
            |          "countryCode": "SKEGNESS",
            |          "taxTakenOff": 200.121,
            |          "specialWithholdingTax": 999999999991.13,
            |          "taxableAmount": -200.14,
            |          "foreignTaxCreditRelief": false
            |       },
            |       {
            |          "amountBeforeTax": -300.11,
            |          "countryCode": "SKEG_",
            |          "taxTakenOff": -300.100,
            |          "specialWithholdingTax": -300.134,
            |          "taxableAmount": -300.14,
            |          "foreignTaxCreditRelief": true
            |       },
            |       {
            |          "amountBeforeTax": -300.11,
            |          "countryCode": "FRE",
            |          "taxTakenOff": -300.100,
            |          "specialWithholdingTax": -300.134,
            |          "taxableAmount": -300.14,
            |          "foreignTaxCreditRelief": true
            |       }
            |    ]
            |}
          """.stripMargin
        )

        val allInvalidValueRequestError: List[MtdError] = List(
          CountryCodeFormatError.copy(
            paths = Some(
              List(
                "/foreignInterest/0/countryCode",
                "/foreignInterest/1/countryCode"
              ))
          ),
          ValueFormatError.copy(
            message = "The value must be between 0 and 99999999999.99",
            paths = Some(
              List(
                "/securities/grossAmount",
                "/securities/taxTakenOff",
                "/securities/netAmount",
                "/foreignInterest/0/taxableAmount",
                "/foreignInterest/0/amountBeforeTax",
                "/foreignInterest/0/taxTakenOff",
                "/foreignInterest/0/specialWithholdingTax",
                "/foreignInterest/1/taxableAmount",
                "/foreignInterest/1/amountBeforeTax",
                "/foreignInterest/1/taxTakenOff",
                "/foreignInterest/1/specialWithholdingTax",
                "/foreignInterest/2/taxableAmount",
                "/foreignInterest/2/amountBeforeTax",
                "/foreignInterest/2/taxTakenOff",
                "/foreignInterest/2/specialWithholdingTax"
              ))
          ),
          RuleCountryCodeError.copy(
            paths = Some(
              List(
                "/foreignInterest/2/countryCode"
              ))
          )
        )

        val wrappedErrors: ErrorWrapper = errors.ErrorWrapper(
          correlationId = correlationId,
          error = BadRequestError,
          errors = Some(allInvalidValueRequestError)
        )

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().put(allInvalidValueRequestBodyJson))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.toJson(wrappedErrors)
      }
    }

    "return error according to spec" when {

      val validRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |  "securities": {
          |        "taxTakenOff": 100.11,
          |        "grossAmount": 100.22,
          |        "netAmount": 100.33
          |      },
          |  "foreignInterest": [
          |     {
          |        "amountBeforeTax": 101.11,
          |        "countryCode": "FRA",
          |        "taxTakenOff": 102.22,
          |        "specialWithholdingTax": 103.33,
          |        "taxableAmount": 104.44,
          |        "foreignTaxCreditRelief": true
          |      },
          |      {
          |        "amountBeforeTax": 201.11,
          |        "countryCode": "DEU",
          |        "taxTakenOff": 202.22,
          |        "specialWithholdingTax": 203.33,
          |        "taxableAmount": 204.44,
          |        "foreignTaxCreditRelief": true
          |      }
          |   ]
          |}
        """.stripMargin
      )

      val invalidCountryCodeRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |  "foreignInterest": [
          |     {
          |        "countryCode": "notACountryCode",
          |        "taxableAmount": 104.44,
          |        "foreignTaxCreditRelief": true
          |      },
          |      {
          |        "countryCode": "notACountryCode",
          |        "taxableAmount": 204.44,
          |        "foreignTaxCreditRelief": true
          |      }
          |   ]
          |}
        """.stripMargin
      )

      val ruleCountryCodeRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |  "foreignInterest": [
          |     {
          |        "countryCode": "FRE",
          |        "taxableAmount": 104.44,
          |        "foreignTaxCreditRelief": true
          |      },
          |      {
          |        "countryCode": "ENL",
          |        "taxableAmount": 204.44,
          |        "foreignTaxCreditRelief": true
          |      }
          |   ]
          |}
        """.stripMargin
      )

      val nonsenseRequestBody: JsValue = Json.parse(
        """
          |{
          |  "field": "value"
          |}
        """.stripMargin
      )

      val nonValidRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |   "securities": {
          |      "taxTakenOff": "no",
          |      "grossAmount": 100.12,
          |      "netAmount": 100.13
          |   },
          |   "foreignInterest": [
          |     {
          |       "countryCode": "DEU",
          |       "foreignTaxCreditRelief": 100,
          |       "taxableAmount": 200.33
          |     }
          |   ]
          |}
        """.stripMargin
      )

      val missingFieldRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |   "foreignInterest": [
          |     {
          |       "countryCode": "DEU",
          |       "foreignTaxCreditRelief": true
          |     }
          |   ]
          |}
        """.stripMargin
      )

      val countryCodeError: MtdError = CountryCodeFormatError.copy(
        paths = Some(
          Seq(
            "/foreignInterest/0/countryCode",
            "/foreignInterest/1/countryCode"
          ))
      )

      val countryCodeRuleError: MtdError = RuleCountryCodeError.copy(
        paths = Some(
          Seq(
            "/foreignInterest/0/countryCode",
            "/foreignInterest/1/countryCode"
          ))
      )

      val allInvalidValueRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |   "securities": {
          |      "taxTakenOff": 100.111,
          |      "grossAmount": -100.12,
          |      "netAmount": 999999999991.99
          |   },
          |   "foreignInterest": [
          |       {
          |          "amountBeforeTax": -200.11,
          |          "countryCode": "GBR",
          |          "taxTakenOff": 200.121,
          |          "specialWithholdingTax": 999999999991.13,
          |          "taxableAmount": -200.14,
          |          "foreignTaxCreditRelief": false
          |       },
          |       {
          |          "amountBeforeTax": -300.11,
          |          "countryCode": "GBR",
          |          "taxTakenOff": -300.100,
          |          "specialWithholdingTax": -300.134,
          |          "taxableAmount": -300.14,
          |          "foreignTaxCreditRelief": true
          |       }
          |    ]
          |}
    """.stripMargin
      )

      val allInvalidValueRequestError: MtdError = ValueFormatError.copy(
        message = "The value must be between 0 and 99999999999.99",
        paths = Some(
          List(
            "/securities/grossAmount",
            "/securities/taxTakenOff",
            "/securities/netAmount",
            "/foreignInterest/0/taxableAmount",
            "/foreignInterest/0/amountBeforeTax",
            "/foreignInterest/0/taxTakenOff",
            "/foreignInterest/0/specialWithholdingTax",
            "/foreignInterest/1/taxableAmount",
            "/foreignInterest/1/amountBeforeTax",
            "/foreignInterest/1/taxTakenOff",
            "/foreignInterest/1/specialWithholdingTax"
          ))
      )

      val nonValidRequestBodyErrors: MtdError = RuleIncorrectOrEmptyBodyError.copy(
        paths = Some(Seq("/foreignInterest/0/foreignTaxCreditRelief", "/securities/taxTakenOff"))
      )

      val missingFieldRequestBodyErrors: MtdError = RuleIncorrectOrEmptyBodyError.copy(
        paths = Some(Seq("/foreignInterest/0/taxableAmount"))
      )

      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedBody: MtdError,
                                scenario: Option[String]): Unit = {
          s"validation fails with ${expectedBody.code} error ${scenario.getOrElse("")}" in new NonTysTest {

            override val nino: String             = requestNino
            override val taxYear: String          = requestTaxYear
            override val requestBodyJson: JsValue = requestBody

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          ("AA1123A", "2019-20", validRequestBodyJson, BAD_REQUEST, NinoFormatError, None),
          ("AA123456A", "20177", validRequestBodyJson, BAD_REQUEST, TaxYearFormatError, None),
          ("AA123456A", "2015-17", validRequestBodyJson, BAD_REQUEST, RuleTaxYearRangeInvalidError, None),
          ("AA123456A", "2016-17", validRequestBodyJson, BAD_REQUEST, RuleTaxYearNotSupportedError, None),
          ("AA123456A", "2019-20", invalidCountryCodeRequestBodyJson, BAD_REQUEST, countryCodeError, None),
          ("AA123456A", "2019-20", ruleCountryCodeRequestBodyJson, BAD_REQUEST, countryCodeRuleError, None),
          ("AA123456A", "2019-20", nonsenseRequestBody, BAD_REQUEST, RuleIncorrectOrEmptyBodyError, None),
          ("AA123456A", "2019-20", allInvalidValueRequestBodyJson, BAD_REQUEST, allInvalidValueRequestError, None),
          ("AA123456A", "2019-20", nonValidRequestBodyJson, BAD_REQUEST, nonValidRequestBodyErrors, Some("(invalid request body format)")),
          ("AA123456A", "2019-20", missingFieldRequestBodyJson, BAD_REQUEST, missingFieldRequestBodyErrors, Some("(missing mandatory fields)"))
        )
        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $downstreamStatus" in new NonTysTest {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.PUT, downstreamUri, downstreamStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        def errorBody(code: String): String =
          s"""
             |{
             |   "code": "$code",
             |   "reason": "downstream message"
             |}
            """.stripMargin

        val errors = Seq(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError)
        )

        val extraTysErrors = Seq(
          (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
          (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError)
        )

        (errors ++ extraTysErrors).foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }

  private trait Test {
    def nino: String = "AA123456A"
    def taxYear: String

    val correlationId: String = "X-123"

    val requestBodyJson: JsValue = Json.parse(
      """
        |{
        |  "securities":
        |      {
        |        "taxTakenOff": 100.11,
        |        "grossAmount": 100.22,
        |        "netAmount": 100.33
        |      },
        |  "foreignInterest":   [
        |     {
        |        "amountBeforeTax": 101.11,
        |        "countryCode": "FRA",
        |        "taxTakenOff": 102.22,
        |        "specialWithholdingTax": 103.33,
        |        "taxableAmount": 104.44,
        |        "foreignTaxCreditRelief": true
        |      },
        |      {
        |        "amountBeforeTax": 201.11,
        |        "countryCode": "DEU",
        |        "taxTakenOff": 202.22,
        |        "specialWithholdingTax": 203.33,
        |        "taxableAmount": 204.44,
        |        "foreignTaxCreditRelief": true
        |      }
        |   ]
        |}
    """.stripMargin
    )

    def mtdUri: String = s"/other/$nino/$taxYear"
    def downstreamUri: String
    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(mtdUri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.1.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

  }

  private trait NonTysTest extends Test {
    def taxYear: String       = "2019-20"
    def downstreamUri: String = s"/income-tax/income/savings/$nino/2019-20"
  }

  private trait TysIfsTest extends Test {
    def taxYear: String       = "2023-24"
    def downstreamUri: String = s"/income-tax/income/savings/23-24/$nino"
  }

}