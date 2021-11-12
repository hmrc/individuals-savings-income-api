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

package v1r7.controllers

import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import v1r7.hateoas.HateoasLinks
import v1r7.mocks.MockIdGenerator
import v1r7.mocks.hateoas.MockHateoasFactory
import v1r7.mocks.requestParsers.MockDeleteRetrieveRequestParser
import v1r7.mocks.services.{MockDeleteRetrieveService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1r7.models.domain.Nino
import v1r7.models.errors.{BadRequestError, DownstreamError, ErrorWrapper, MtdError, NinoFormatError, NotFoundError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, TaxYearFormatError}
import v1r7.models.hateoas.{HateoasWrapper, Link}
import v1r7.models.hateoas.Method.{DELETE, GET, PUT}
import v1r7.models.hateoas.RelType.{CREATE_AND_AMEND_OTHER_CGT_AND_DISPOSALS, DELETE_OTHER_CGT_AND_DISPOSALS, SELF}
import v1r7.models.outcomes.ResponseWrapper
import v1r7.models.request.{DeleteRetrieveRawData, DeleteRetrieveRequest}
import v1r7.models.response.retrieveOtherCgt.{Disposal, Losses, NonStandardGains, RetrieveOtherCgtHateoasData, RetrieveOtherCgtResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveOtherCgtControllerSpec extends ControllerBaseSpec
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockDeleteRetrieveService
  with MockHateoasFactory
  with MockDeleteRetrieveRequestParser
  with HateoasLinks
  with MockIdGenerator {

  val nino: String = "AA123456A"
  val taxYear: String = "2019-20"
  val correlationId: String = "X-123"

  val rawData: DeleteRetrieveRawData = DeleteRetrieveRawData(
    nino = nino,
    taxYear = taxYear
  )

  val requestData: DeleteRetrieveRequest = DeleteRetrieveRequest(
    nino = Nino(nino),
    taxYear = taxYear
  )

  val amendOtherCgtLink: Link =
    Link(
      href = s"/individuals/income-received/disposals/other-gains/$nino/$taxYear",
      method = PUT,
      rel = CREATE_AND_AMEND_OTHER_CGT_AND_DISPOSALS
    )

  val retrieveOtherCgtLink: Link =
    Link(
      href = s"/individuals/income-received/disposals/other-gains/$nino/$taxYear",
      method = GET,
      rel = SELF
    )

  val deleteOtherCgtLink: Link =
    Link(
      href = s"/individuals/income-received/disposals/other-gains/$nino/$taxYear",
      method = DELETE,
      rel = DELETE_OTHER_CGT_AND_DISPOSALS
    )

  val responseModel: RetrieveOtherCgtResponse = RetrieveOtherCgtResponse(
    submittedOn = "2021-05-07T16:18:44.403Z",
    disposals = Some(Seq(
      Disposal(
        assetType = "otherProperty",
        assetDescription = "string",
        acquisitionDate = "2021-05-07",
        disposalDate = "2021-05-07",
        disposalProceeds = 59999999999.99,
        allowableCosts = 59999999999.99,
        gain = Some(59999999999.99),
        loss = None,
        claimOrElectionCodes = Some(Seq("OTH")),
        gainAfterRelief = Some(59999999999.99),
        lossAfterRelief = None,
        rttTaxPaid = Some(59999999999.99)
      )
    )),
    nonStandardGains = Some(
      NonStandardGains(
        carriedInterestGain = Some(19999999999.99),
        carriedInterestRttTaxPaid = Some(19999999999.99),
        attributedGains = Some(19999999999.99),
        attributedGainsRttTaxPaid = Some(19999999999.99),
        otherGains = Some(19999999999.99),
        otherGainsRttTaxPaid = Some(19999999999.99)
      )
    ),
    losses = Some(
      Losses(
        broughtForwardLossesUsedInCurrentYear = Some(29999999999.99),
        setAgainstInYearGains = Some(29999999999.99),
        setAgainstInYearGeneralIncome = Some(29999999999.99),
        setAgainstEarlierYear = Some(29999999999.99)
      )
    ),
    adjustments = Some(-39999999999.99)
  )

  val validResponseJson: JsValue = Json.parse(
    """
      |{
      |   "submittedOn":"2021-05-07T16:18:44.403Z",
      |   "disposals":[
      |      {
      |         "assetType":"otherProperty",
      |         "assetDescription":"string",
      |         "acquisitionDate":"2021-05-07",
      |         "disposalDate":"2021-05-07",
      |         "disposalProceeds":59999999999.99,
      |         "allowableCosts":59999999999.99,
      |         "gain":59999999999.99,
      |         "claimOrElectionCodes":[
      |            "OTH"
      |         ],
      |         "gainAfterRelief":59999999999.99,
      |         "rttTaxPaid":59999999999.99
      |      }
      |   ],
      |   "nonStandardGains":{
      |      "carriedInterestGain":19999999999.99,
      |      "carriedInterestRttTaxPaid":19999999999.99,
      |      "attributedGains":19999999999.99,
      |      "attributedGainsRttTaxPaid":19999999999.99,
      |      "otherGains":19999999999.99,
      |      "otherGainsRttTaxPaid":19999999999.99
      |   },
      |   "losses":{
      |      "broughtForwardLossesUsedInCurrentYear":29999999999.99,
      |      "setAgainstInYearGains":29999999999.99,
      |      "setAgainstInYearGeneralIncome":29999999999.99,
      |      "setAgainstEarlierYear":29999999999.99
      |   },
      |   "adjustments":-39999999999.99
      |}
     """.stripMargin
  )

  val mtdResponse: JsObject = validResponseJson.as[JsObject] ++ Json.parse(
    s"""
       |{
       |   "links":[
       |      {
       |         "href":"/individuals/income-received/disposals/other-gains/$nino/$taxYear",
       |         "method":"PUT",
       |         "rel":"create-and-amend-other-capital-gains-and-disposals"
       |      },
       |      {
       |         "href":"/individuals/income-received/disposals/other-gains/$nino/$taxYear",
       |         "method":"GET",
       |         "rel":"self"
       |      },
       |      {
       |         "href":"/individuals/income-received/disposals/other-gains/$nino/$taxYear",
       |         "method":"DELETE",
       |         "rel":"delete-other-capital-gains-and-disposals"
       |      }
       |   ]
       |}
    """.stripMargin
  ).as[JsObject]

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new RetrieveOtherCgtController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      requestParser = mockDeleteRetrieveRequestParser,
      service = mockDeleteRetrieveService,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockIdGenerator.generateCorrelationId.returns(correlationId)
  }

  "RetrieveOtherCgtController" should {
    "return OK" when {
      "happy path" in new Test {

        MockDeleteRetrieveRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockDeleteRetrieveService
          .retrieve[RetrieveOtherCgtResponse](defaultDownstreamErrorMap)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseModel))))

        MockHateoasFactory
          .wrap(responseModel, RetrieveOtherCgtHateoasData(nino, taxYear))
          .returns(HateoasWrapper(responseModel,
            Seq(
              amendOtherCgtLink,
              retrieveOtherCgtLink,
              deleteOtherCgtLink
            )
          ))

        val result: Future[Result] = controller.retrieveOtherCgt(nino, taxYear)(fakeGetRequest)

        status(result) shouldBe OK
        contentAsJson(result) shouldBe mtdResponse
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return the error as per spec" when {
      "parser errors occur" must {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockDeleteRetrieveRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(correlationId, error, None)))

            val result: Future[Result] = controller.retrieveOtherCgt(nino, taxYear)(fakeGetRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)
          }
        }

        val input = Seq(
          (BadRequestError, BAD_REQUEST),
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (RuleTaxYearNotSupportedError, BAD_REQUEST),
          (RuleTaxYearRangeInvalidError, BAD_REQUEST)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" must {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockDeleteRetrieveRequestParser
              .parse(rawData)
              .returns(Right(requestData))

            MockDeleteRetrieveService
              .retrieve[RetrieveOtherCgtResponse](defaultDownstreamErrorMap)
              .returns(Future.successful(Left(ErrorWrapper(correlationId, mtdError))))

            val result: Future[Result] = controller.retrieveOtherCgt(nino, taxYear)(fakeGetRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)
          }
        }

        val input = Seq(
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (NotFoundError, NOT_FOUND),
          (DownstreamError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }
}