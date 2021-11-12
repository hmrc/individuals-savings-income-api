/*
 * Copyright 2021 HM Revenue & Customs
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

package v1r7.endpoints

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.libs.ws.{ WSRequest, WSResponse }
import support.{ V1R7IntegrationSpec, WireMockMethods }
import v1r7.models.errors._
import v1r7.stubs.{ AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub }

class CreateAmendCgtPpdOverridesControllerISpec extends V1R7IntegrationSpec with WireMockMethods {

  val validRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |    "multiplePropertyDisposals": [
      |         {
      |            "ppdSubmissionId": "AB0000000092",
      |            "amountOfNetGain": 1234.78
      |         },
      |         {
      |            "ppdSubmissionId": "AB0000000098",
      |            "amountOfNetLoss": 134.99
      |         }
      |    ],
      |    "singlePropertyDisposals": [
      |         {
      |             "ppdSubmissionId": "AB0000000099",
      |             "completionDate": "2020-02-28",
      |             "disposalProceeds": 454.24,
      |             "acquisitionDate": "2020-03-29",
      |             "acquisitionAmount": 3434.45,
      |             "improvementCosts": 233.45,
      |             "additionalCosts": 423.34,
      |             "prfAmount": 2324.67,
      |             "otherReliefAmount": 3434.23,
      |             "lossesFromThisYear": 436.23,
      |             "lossesFromPreviousYear": 234.23,
      |             "amountOfNetGain": 4567.89
      |         },
      |         {
      |             "ppdSubmissionId": "AB0000000091",
      |             "completionDate": "2020-02-28",
      |             "disposalProceeds": 454.24,
      |             "acquisitionDate": "2020-03-29",
      |             "acquisitionAmount": 3434.45,
      |             "improvementCosts": 233.45,
      |             "additionalCosts": 423.34,
      |             "prfAmount": 2324.67,
      |             "otherReliefAmount": 3434.23,
      |             "lossesFromThisYear": 436.23,
      |             "lossesFromPreviousYear": 234.23,
      |             "amountOfNetLoss": 4567.89
      |         }
      |    ]
      |}
      |""".stripMargin
  )

  val nonsenseBodyJson: JsValue = Json.parse(
    """
      |{
      |  "aField": "aValue"
      |}
       """.stripMargin
  )

  val emptyFieldsJson: JsValue = Json.parse(
    """
      |{
      |   "multiplePropertyDisposals": [],
      |   "singlePropertyDisposals": []
      |}
     """.stripMargin
  )

  val emptyFieldsError: MtdError = RuleIncorrectOrEmptyBodyError.copy(
    paths = Some(
      Seq(
        "/multiplePropertyDisposals",
        "/singlePropertyDisposals"
      ))
  )

  val missingFieldsJson: JsValue = Json.parse(
    """
      |{
      |   "multiplePropertyDisposals": [{}],
      |   "singlePropertyDisposals": [{}]
      |}
     """.stripMargin
  )

  val missingFieldsError: MtdError = RuleIncorrectOrEmptyBodyError.copy(
    paths = Some(
      Seq(
        "/singlePropertyDisposals/0/otherReliefAmount",
        "/singlePropertyDisposals/0/disposalProceeds",
        "/singlePropertyDisposals/0/prfAmount",
        "/multiplePropertyDisposals/0/ppdSubmissionId",
        "/singlePropertyDisposals/0/improvementCosts",
        "/singlePropertyDisposals/0/completionDate",
        "/singlePropertyDisposals/0/ppdSubmissionId",
        "/singlePropertyDisposals/0/additionalCosts",
        "/singlePropertyDisposals/0/acquisitionAmount"
      ))
  )

  val gainAndLossJson: JsValue = Json.parse(
    """
      |{
      |    "multiplePropertyDisposals": [
      |         {
      |            "ppdSubmissionId": "AB0000000092",
      |            "amountOfNetGain": 1234.78,
      |            "amountOfNetLoss": 134.99
      |         }
      |    ],
      |    "singlePropertyDisposals": [
      |         {
      |             "ppdSubmissionId": "AB0000000098",
      |             "completionDate": "2020-02-28",
      |             "disposalProceeds": 454.24,
      |             "acquisitionDate": "2020-03-29",
      |             "acquisitionAmount": 3434.45,
      |             "improvementCosts": 233.45,
      |             "additionalCosts": 423.34,
      |             "prfAmount": 2324.67,
      |             "otherReliefAmount": 3434.23,
      |             "lossesFromThisYear": 436.23,
      |             "lossesFromPreviousYear": 234.23,
      |             "amountOfNetGain": 4567.89,
      |             "amountOfNetLoss": 50000.99
      |         }
      |    ]
      |}
      |""".stripMargin
  )

  val amountGainLossError: MtdError = RuleAmountGainLossError.copy(
    paths = Some(
      Seq(
        "/multiplePropertyDisposals/0",
        "/singlePropertyDisposals/0"
      ))
  )

  val lossGreaterThanGainJson: JsValue = Json.parse(
    """
      |{
      |    "multiplePropertyDisposals": [
      |         {
      |            "ppdSubmissionId": "AB0000000092",
      |            "amountOfNetGain": 1234.78
      |         }
      |    ],
      |    "singlePropertyDisposals": [
      |         {
      |             "ppdSubmissionId": "AB0000000098",
      |             "completionDate": "2020-02-28",
      |             "disposalProceeds": 454.24,
      |             "acquisitionDate": "2020-03-29",
      |             "acquisitionAmount": 3434.45,
      |             "improvementCosts": 233.45,
      |             "additionalCosts": 423.34,
      |             "prfAmount": 2324.67,
      |             "otherReliefAmount": 3434.23,
      |             "lossesFromThisYear": 6464.99,
      |             "lossesFromPreviousYear": 234.23,
      |             "amountOfNetGain": 4567.89
      |         }
      |    ]
      |}
      |""".stripMargin
  )

  val lossesGreaterThanGainError: MtdError = RuleLossesGreaterThanGainError.copy(
    paths = Some(
      Seq(
        "/singlePropertyDisposals/0/lossesFromThisYear"
      ))
  )

  private val invalidValueRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |    "multiplePropertyDisposals": [
      |         {
      |            "ppdSubmissionId": "AB0000000092",
      |            "amountOfNetGain": 1234.787385
      |         },
      |         {
      |            "ppdSubmissionId": "AB0000000092",
      |            "amountOfNetLoss": -134.99
      |         }
      |    ],
      |    "singlePropertyDisposals": [
      |         {
      |             "ppdSubmissionId": "AB0000000092",
      |             "completionDate": "2020-02-28",
      |             "disposalProceeds": 454.24999,
      |             "acquisitionDate": "2020-03-29",
      |             "acquisitionAmount": 3434.45346,
      |             "improvementCosts": 233.4628,
      |             "additionalCosts": 423.34829,
      |             "prfAmount": -2324.67,
      |             "otherReliefAmount": -3434.23,
      |             "lossesFromThisYear": 436.23297423,
      |             "lossesFromPreviousYear": 234.2334728,
      |             "amountOfNetGain": 4567.8974726
      |         },
      |         {
      |             "ppdSubmissionId": "AB0000000092",
      |             "completionDate": "2020-02-28",
      |             "disposalProceeds": -454.24,
      |             "acquisitionDate": "2020-03-29",
      |             "acquisitionAmount": 3434.45837,
      |             "improvementCosts": 233.4628,
      |             "additionalCosts": -423.34,
      |             "prfAmount": 2324.678372,
      |             "otherReliefAmount": -3434.23,
      |             "lossesFromThisYear": 436.23287,
      |             "lossesFromPreviousYear": -234.23,
      |             "amountOfNetLoss": 4567.8983724
      |         }
      |    ]
      |}
      |""".stripMargin
  )

  val invalidValueErrors: MtdError = ValueFormatError.copy(
    message = "The value must be between 0 and 99999999999.99",
    paths = Some(
      Seq(
        "/multiplePropertyDisposals/0/amountOfNetGain",
        "/multiplePropertyDisposals/1/amountOfNetLoss",
        "/singlePropertyDisposals/0/disposalProceeds",
        "/singlePropertyDisposals/0/acquisitionAmount",
        "/singlePropertyDisposals/0/improvementCosts",
        "/singlePropertyDisposals/0/additionalCosts",
        "/singlePropertyDisposals/0/prfAmount",
        "/singlePropertyDisposals/0/otherReliefAmount",
        "/singlePropertyDisposals/0/lossesFromThisYear",
        "/singlePropertyDisposals/0/lossesFromPreviousYear",
        "/singlePropertyDisposals/0/amountOfNetGain",
        "/singlePropertyDisposals/1/disposalProceeds",
        "/singlePropertyDisposals/1/acquisitionAmount",
        "/singlePropertyDisposals/1/improvementCosts",
        "/singlePropertyDisposals/1/additionalCosts",
        "/singlePropertyDisposals/1/prfAmount",
        "/singlePropertyDisposals/1/otherReliefAmount",
        "/singlePropertyDisposals/1/lossesFromThisYear",
        "/singlePropertyDisposals/1/lossesFromPreviousYear",
        "/singlePropertyDisposals/1/amountOfNetLoss"
      ))
  )

  def jsonWithIds(multipleSubmissionId: String, singleSubmissionId: String): JsValue = Json.parse(
    s"""
      |{
      |    "multiplePropertyDisposals": [
      |         {
      |            "ppdSubmissionId": "$multipleSubmissionId",
      |            "amountOfNetGain": 1234.78
      |         }
      |    ],
      |    "singlePropertyDisposals": [
      |         {
      |             "ppdSubmissionId": "$singleSubmissionId",
      |             "completionDate": "2020-02-28",
      |             "disposalProceeds": 454.24,
      |             "acquisitionDate": "2020-03-29",
      |             "acquisitionAmount": 3434.45,
      |             "improvementCosts": 233.45,
      |             "additionalCosts": 423.34,
      |             "prfAmount": 2324.67,
      |             "otherReliefAmount": 3434.23,
      |             "lossesFromThisYear": 436.23,
      |             "lossesFromPreviousYear": 234.23,
      |             "amountOfNetGain": 4567.89
      |         }
      |    ]
      |}
      |""".stripMargin
  )

  val ppdSubmissionFormatError: MtdError = PpdSubmissionIdFormatError.copy(
    paths = Some(
      Seq(
        "/multiplePropertyDisposals/0/ppdSubmissionId",
        "/singlePropertyDisposals/0/ppdSubmissionId",
      ))
  )

  def ppdDuplicatedIdError(duplicatedId: String): MtdError = RuleDuplicatedPpdSubmissionIdError.forDuplicatedIdAndPaths(
    id = duplicatedId,
    paths = Seq(
      "/multiplePropertyDisposals/0/ppdSubmissionId",
      "/singlePropertyDisposals/0/ppdSubmissionId",
    )
  )

  val invalidDateFormatJson: JsValue = Json.parse(
    """
      |{
      |    "multiplePropertyDisposals": [
      |         {
      |            "ppdSubmissionId": "AB0000000091",
      |            "amountOfNetGain": 1234.78
      |         }
      |    ],
      |    "singlePropertyDisposals": [
      |         {
      |             "ppdSubmissionId": "AB0000000092",
      |             "completionDate": "20-02-28",
      |             "disposalProceeds": 454.24,
      |             "acquisitionDate": "2003-29",
      |             "acquisitionAmount": 3434.45,
      |             "improvementCosts": 233.45,
      |             "additionalCosts": 423.34,
      |             "prfAmount": 2324.67,
      |             "otherReliefAmount": 3434.23,
      |             "lossesFromThisYear": 436.23,
      |             "lossesFromPreviousYear": 234.23,
      |             "amountOfNetGain": 4567.89
      |         }
      |    ]
      |}
      |""".stripMargin
  )

  val dateFormatError: MtdError = DateFormatError.copy(
    paths = Some(
      Seq(
        "/singlePropertyDisposals/0/completionDate",
        "/singlePropertyDisposals/0/acquisitionDate"
      ))
  )

  private trait Test {

    val nino: String    = "AA123456A"
    val taxYear: String = "2020-21"

    val hateoasResponse: JsValue = Json.parse(
      s"""
         |{
         |   "links":[
         |      {
         |         "href":"/individuals/income-received/disposals/residential-property/$nino/$taxYear/ppd",
         |         "method":"PUT",
         |         "rel":"create-and-amend-report-and-pay-capital-gains-tax-on-property-overrides"
         |      },
         |      {
         |         "href":"/individuals/income-received/disposals/residential-property/$nino/$taxYear/ppd",
         |         "method":"DELETE",
         |         "rel":"delete-report-and-pay-capital-gains-tax-on-property-overrides"
         |      },
         |      {
         |         "href":"/individuals/income-received/disposals/residential-property/$nino/$taxYear",
         |         "method":"GET",
         |         "rel":"self"
         |      }
         |   ]
         |}
    """.stripMargin
    )

    def uri: String = s"/disposals/residential-property/$nino/$taxYear/ppd "

    def ifsUri: String = s"/income-tax/income/disposals/residential-property/ppd/$nino/$taxYear"

    def setupStubs(): StubMapping

    def request: WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }

    def verifyNrs(payload: JsValue): Unit =
      verify(
        postRequestedFor(urlEqualTo(s"/mtd-api-nrs-proxy/$nino/itsa-cgt-disposal-ppd"))
          .withRequestBody(equalToJson(payload.toString())))
  }

  "Calling Create and Amend 'Report and Pay Capital Gains Tax on Property' Overrides endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.PUT, ifsUri, NO_CONTENT)
        }

        val response: WSResponse = await(request.put(validRequestBodyJson))
        response.status shouldBe OK
        response.body[JsValue] shouldBe hateoasResponse
        response.header("Content-Type") shouldBe Some("application/json")

        verifyNrs(validRequestBodyJson)
      }
    }

    "return a 400 with multiple errors" when {
      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedError: MtdError,
                                expectedErrors: Option[ErrorWrapper],
                                scenario: Option[String]): Unit = {
          s"validation fails with ${expectedError.code} error${scenario.fold("")(scenario => s" for $scenario scenario")}" in new Test {
            override val nino: String    = requestNino
            override val taxYear: String = requestTaxYear

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
            }

            val response: WSResponse = await(request.put(requestBody))
            response.status shouldBe expectedStatus
            response.json shouldBe expectedErrors.fold(Json.toJson(expectedError))(errorWrapper => Json.toJson(errorWrapper))
            response.header("Content-Type") shouldBe Some("application/json")
          }
        }

        val input = Seq(
          // Path errors
          ("AA1123A", "2020-21", validRequestBodyJson, BAD_REQUEST, NinoFormatError, None, None),
          ("AA123456A", "20177", validRequestBodyJson, BAD_REQUEST, TaxYearFormatError, None, None),
          ("AA123456A", "2015-17", validRequestBodyJson, BAD_REQUEST, RuleTaxYearRangeInvalidError, None, None),
          ("AA123456A", "2018-19", validRequestBodyJson, BAD_REQUEST, RuleTaxYearNotSupportedError, None, None),
          // Body Errors
          ("AA123456A", "2020-21", JsObject.empty, BAD_REQUEST, RuleIncorrectOrEmptyBodyError, None, Some("emptyBody")),
          ("AA123456A", "2020-21", nonsenseBodyJson, BAD_REQUEST, RuleIncorrectOrEmptyBodyError, None, Some("nonsenseBody")),
          ("AA123456A", "2020-21", emptyFieldsJson, BAD_REQUEST, emptyFieldsError, None, Some("emptyFields")),
          ("AA123456A", "2020-21", missingFieldsJson, BAD_REQUEST, missingFieldsError, None, Some("missingFields")),
          ("AA123456A", "2020-21", gainAndLossJson, BAD_REQUEST, amountGainLossError, None, Some("gainAndLossRule")),
          ("AA123456A", "2020-21", invalidDateFormatJson, BAD_REQUEST, dateFormatError, None, Some("dateFormat")),
          ("AA123456A", "2020-21", lossGreaterThanGainJson, BAD_REQUEST, lossesGreaterThanGainError, None, Some("lossesGreaterThanGainsRule")),
          ("AA123456A", "2020-21", invalidValueRequestBodyJson, BAD_REQUEST, invalidValueErrors, None, Some("invalidNumValues")),
          ("AA123456A", "2020-21", jsonWithIds("notAnID", "notAnID"), BAD_REQUEST, ppdSubmissionFormatError, None, Some("badIDs")),
          ("AA123456A",
           "2020-21",
           jsonWithIds("DuplicatedId", "DuplicatedId"),
           BAD_REQUEST,
           ppdDuplicatedIdError("DuplicatedId"),
           None,
           Some("duplicatedIDs")),
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

            val response: WSResponse = await(request.put(validRequestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
            response.header("Content-Type") shouldBe Some("application/json")

            verifyNrs(validRequestBodyJson)
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
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, DownstreamError),
          (NOT_FOUND, "PPD_SUBMISSIONID_NOT_FOUND", NOT_FOUND, PpdSubmissionIdNotFoundError),
          (NOT_FOUND, "NO_PPD_SUBMISSIONS_FOUND", NOT_FOUND, NotFoundError),
          (CONFLICT, "DUPLICATE_SUBMISSION", BAD_REQUEST, RuleDuplicatedPpdSubmissionIdError),
          (UNPROCESSABLE_ENTITY, "INVALID_REQUEST_BEFORE_TAX_YEAR", BAD_REQUEST, RuleTaxYearNotEndedError),
          (UNPROCESSABLE_ENTITY, "INVALID_DISPOSAL_TYPE", FORBIDDEN, RuleIncorrectDisposalTypeError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, DownstreamError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, DownstreamError)
        )

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}