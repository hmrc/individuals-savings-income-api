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

package v1.endpoints

import api.stubs.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import api.models.errors.{CessationDateFormatError, EmployerNameFormatError, EmployerRefFormatError, EmploymentIdFormatError, MtdError, NinoFormatError, NotFoundError, PayrollIdFormatError, RuleCessationDateBeforeStartDateError, RuleCessationDateBeforeTaxYearStartError, RuleIncorrectOrEmptyBodyError, RuleStartDateAfterTaxYearEndError, RuleTaxYearNotEndedError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, RuleUpdateForbiddenError, StandardDownstreamError, StartDateFormatError, TaxYearFormatError}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import support.IntegrationBaseSpec

class AmendCustomEmploymentControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String = "AA123456A"
    val taxYear: String = "2019-20"
    val employmentId = "4557ecb5-fd32-48cc-81f5-e6acd1099f3c"

    val requestBodyJson: JsValue = Json.parse(
      """
        |{
        |  "employerRef": "123/AZ12334",
        |  "employerName": "AMD infotech Ltd",
        |  "startDate": "2019-01-01",
        |  "cessationDate": "2020-06-01",
        |  "payrollId": "124214112412",
        |  "occupationalPension": false
        |}
    """.stripMargin
    )

    def uri: String = s"/employments/$nino/$taxYear/$employmentId"

    def ifsUri: String = s"/income-tax/income/employments/$nino/$taxYear/custom/$employmentId"

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

  "Calling the amend custom employment endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, ifsUri, NO_CONTENT)
        }

        val hateoasResponse: JsValue = Json.parse(
          s"""
             |{
             |   "links":[
             |      {
             |         "href":"/individuals/income-received/employments/$nino/$taxYear",
             |         "rel":"list-employments",
             |         "method":"GET"
             |      },
             |      {
             |         "href":"/individuals/income-received/employments/$nino/$taxYear/$employmentId",
             |         "rel":"self",
             |         "method":"GET"
             |      },
             |      {
             |         "href":"/individuals/income-received/employments/$nino/$taxYear/$employmentId",
             |         "rel":"amend-custom-employment",
             |         "method":"PUT"
             |      },
             |      {
             |         "href":"/individuals/income-received/employments/$nino/$taxYear/$employmentId",
             |         "rel":"delete-custom-employment",
             |         "method":"DELETE"
             |      }
             |   ]
             |}
    """.stripMargin
        )

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
        response.body[JsValue] shouldBe hateoasResponse
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return a 400 with multiple errors" when {
      "all field value validations fail on the request body" in new Test {

        private val amendCustomEmploymentInvalidRequest = Json.parse(
          s"""
             |{
             |  "employerRef": "1234/AB56797",
             |  "employerName": "asasfdsgdgdsgdffdhfhgfjghjhgkhkhjgkfgdsfsfsfasfsafsesgsdgdsgdfgsdgdsgdsgdsgsdgsdgsdgdsgsdgsdgdsgsdgs",
             |  "startDate": "2019-01-013",
             |  "cessationDate": "2020-06-013",
             |  "payrollId": "123addusersitepackagesaddusersitepackagesaddusersitepackagesaddusersitepackagesaddusersitepackages",
             |  "occupationalPension": false
             |}
             |""".stripMargin)

        private val responseJson = Json.parse(
          """
            |{
            |	"code": "INVALID_REQUEST",
            |	"message": "Invalid request",
            |	"errors": [{
            |		"code": "FORMAT_EMPLOYER_REF",
            |		"message": "The provided employer ref is invalid"
            |	}, {
            |		"code": "FORMAT_EMPLOYER_NAME",
            |		"message": "The provided employer name is invalid"
            |	}, {
            |		"code": "FORMAT_START_DATE",
            |		"message": "The provided start date is invalid"
            |	}, {
            |		"code": "FORMAT_CESSATION_DATE",
            |		"message": "The provided cessation date is invalid"
            |	}, {
            |		"code": "FORMAT_PAYROLL_ID",
            |		"message": "The provided payroll ID is invalid"
            |	}]
            |}
            |""".stripMargin)

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().put(amendCustomEmploymentInvalidRequest))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe responseJson
      }
    }

    "return error according to spec" when {

      val validRequestJson: JsValue = Json.parse(
        """
          |{
          |  "employerRef": "123/AZ12334",
          |  "employerName": "AMD infotech Ltd",
          |  "startDate": "2019-01-01",
          |  "cessationDate": "2020-06-01",
          |  "payrollId": "124214112412",
          |  "occupationalPension": false
          |}
      """.stripMargin
      )

      val emptyRequestJson: JsValue = JsObject.empty

      val nonValidRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |  "employerRef": false,
          |  "employerName": false,
          |  "startDate": false,
          |  "cessationDate": false,
          |  "payrollId": false,
          |  "occupationalPension": 77
          |}
        """.stripMargin
      )

      val missingFieldRequestBodyJson: JsValue = Json.parse(
        """
          |{
          |  "employerRef": "123/AZ12334"
          |}
        """.stripMargin
      )

      val invalidEmployerRefRequestJson: JsValue = Json.parse(
        """
          |{
          |  "employerRef": "notValid",
          |  "employerName": "AMD infotech Ltd",
          |  "startDate": "2019-01-01",
          |  "cessationDate": "2020-06-01",
          |  "payrollId": "124214112412",
          |  "occupationalPension": false
          |}
      """.stripMargin
      )

      val invalidEmployerNameRequestJson: JsValue = Json.parse(
        s"""
           |{
           |  "employerRef": "123/AZ12334",
           |  "employerName": "${"a" * 100}",
           |  "startDate": "2019-01-01",
           |  "cessationDate": "2020-06-01",
           |  "payrollId": "124214112412",
           |  "occupationalPension": false
           |}
      """.stripMargin
      )

      val invalidStartDateRequestJson: JsValue = Json.parse(
        """
          |{
          |  "employerRef": "123/AZ12334",
          |  "employerName": "AMD infotech Ltd",
          |  "startDate": "notValid",
          |  "cessationDate": "2020-06-01",
          |  "payrollId": "124214112412",
          |  "occupationalPension": false
          |}
      """.stripMargin
      )

      val invalidCessationDateRequestJson: JsValue = Json.parse(
        """
          |{
          |  "employerRef": "123/AZ12334",
          |  "employerName": "AMD infotech Ltd",
          |  "startDate": "2019-01-01",
          |  "cessationDate": "notValid",
          |  "payrollId": "124214112412",
          |  "occupationalPension": false
          |}
      """.stripMargin
      )

      val invalidPayrollIdRequestJson: JsValue = Json.parse(
        s"""
           |{
           |  "employerRef": "123/AZ12334",
           |  "employerName": "AMD infotech Ltd",
           |  "startDate": "2019-01-01",
           |  "cessationDate": "2020-06-01",
           |  "payrollId": "${"a" * 100}",
           |  "occupationalPension": false
           |}
      """.stripMargin
      )

      val invalidDateOrderRequestJson: JsValue = Json.parse(
        """
          |{
          |  "employerRef": "123/AZ12334",
          |  "employerName": "AMD infotech Ltd",
          |  "startDate": "2020-01-01",
          |  "cessationDate": "2019-06-01",
          |  "payrollId": "124214112412",
          |  "occupationalPension": false
          |}
      """.stripMargin
      )

      val startDateLateRequestJson: JsValue = Json.parse(
        """
          |{
          |  "employerRef": "123/AZ12334",
          |  "employerName": "AMD infotech Ltd",
          |  "startDate": "2023-01-01",
          |  "cessationDate": "2023-06-01",
          |  "payrollId": "124214112412",
          |  "occupationalPension": false
          |}
      """.stripMargin
      )

      val cessationDateEarlyRequestJson: JsValue = Json.parse(
        """
          |{
          |  "employerRef": "123/AZ12334",
          |  "employerName": "AMD infotech Ltd",
          |  "startDate": "2018-01-01",
          |  "cessationDate": "2018-06-01",
          |  "payrollId": "124214112412",
          |  "occupationalPension": false
          |}
      """.stripMargin
      )

      val invalidFieldType: MtdError = RuleIncorrectOrEmptyBodyError.copy(
        paths = Some(
          List(
            "/employerRef",
            "/employerName",
            "/payrollId",
            "/cessationDate",
            "/startDate",
            "/occupationalPension"
          ))
      )

      val missingMandatoryFieldErrors: MtdError = RuleIncorrectOrEmptyBodyError.copy(
        paths = Some(
          List(
            "/employerName",
            "/startDate",
            "/occupationalPension"
          ))
      )

      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestEmploymentId: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedBody: MtdError,
                                scenario: Option[String]): Unit = {
          s"validation fails with ${expectedBody.code} error ${scenario.getOrElse("")}" in new Test {

            override val nino: String = requestNino
            override val taxYear: String = requestTaxYear
            override val employmentId: String = requestEmploymentId
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
          ("AA1123A", "2019-20", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validRequestJson, BAD_REQUEST, NinoFormatError, None),
          ("AA123456A", "20177", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", validRequestJson, BAD_REQUEST, TaxYearFormatError, None),
          ("AA123456A", "2019-20", "ABCDEFG", validRequestJson, BAD_REQUEST, EmploymentIdFormatError, None),
          ("AA123456A", "2015-17", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validRequestJson, BAD_REQUEST, RuleTaxYearRangeInvalidError, None),
          ("AA123456A", "2015-16", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", validRequestJson, BAD_REQUEST, RuleTaxYearNotSupportedError, None),
          ("AA123456A", getCurrentTaxYear, "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", validRequestJson, BAD_REQUEST, RuleTaxYearNotEndedError, None),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", invalidEmployerRefRequestJson, BAD_REQUEST, EmployerRefFormatError, None),
          ("AA123456A", "2019-20", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", invalidEmployerNameRequestJson, BAD_REQUEST, EmployerNameFormatError, None),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", invalidPayrollIdRequestJson, BAD_REQUEST, PayrollIdFormatError, None),
          ("AA123456A", "2019-20", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", invalidStartDateRequestJson, BAD_REQUEST, StartDateFormatError, None),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", invalidCessationDateRequestJson, BAD_REQUEST, CessationDateFormatError, None),
          ("AA123456A", "2019-20", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", invalidDateOrderRequestJson, BAD_REQUEST, RuleCessationDateBeforeStartDateError, None),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", startDateLateRequestJson, BAD_REQUEST, RuleStartDateAfterTaxYearEndError, None),
          ("AA123456A", "2019-20", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", cessationDateEarlyRequestJson, BAD_REQUEST, RuleCessationDateBeforeTaxYearStartError, None),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", emptyRequestJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError, None),
          ("AA123456A", "2019-20", "4557ecb5-fd32-48cc-81f5-e6acd1099f3c", nonValidRequestBodyJson, BAD_REQUEST, invalidFieldType, Some("(invalid field type)")),
          ("AA123456A", "2019-20", "78d9f015-a8b4-47a8-8bbc-c253a1e8057e", missingFieldRequestBodyJson, BAD_REQUEST, missingMandatoryFieldErrors, Some("(missing mandatory fields)"))
        )
        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "ifs service error" when {
        def serviceErrorTest(ifsStatus: Int, ifsCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"ifs returns an $ifsCode error and status $ifsStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.PUT, ifsUri, ifsStatus, errorBody(ifsCode))
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
             |   "reason": "ifs message"
             |}
            """.stripMargin

        val input = Seq(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (UNPROCESSABLE_ENTITY, "NOT_SUPPORTED_TAX_YEAR", BAD_REQUEST, RuleTaxYearNotEndedError),
          (UNPROCESSABLE_ENTITY, "INVALID_DATE_RANGE", BAD_REQUEST, RuleStartDateAfterTaxYearEndError),
          (UNPROCESSABLE_ENTITY, "INVALID_CESSATION_DATE", BAD_REQUEST, RuleCessationDateBeforeTaxYearStartError),
          (UNPROCESSABLE_ENTITY, "CANNOT_UPDATE", FORBIDDEN, RuleUpdateForbiddenError),
          (BAD_REQUEST, "INVALID_EMPLOYMENT_ID", BAD_REQUEST, EmploymentIdFormatError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (NOT_FOUND, "NO_DATA_FOUND", NOT_FOUND, NotFoundError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, StandardDownstreamError)
        )
        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}