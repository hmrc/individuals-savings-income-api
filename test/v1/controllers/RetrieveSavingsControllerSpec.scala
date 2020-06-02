/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.controllers

import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.fixtures.RetrieveSavingsFixture
import v1.hateoas.HateoasLinks
import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockDeleteRetrieveRequestParser
import v1.mocks.services.{MockEnrolmentsAuthService, MockMtdIdLookupService, MockRetrieveSavingsService}
import v1.models.domain.DesTaxYear
import v1.models.errors._
import v1.models.hateoas.Method.{DELETE, GET, PUT}
import v1.models.hateoas.RelType.{AMEND_SAVINGS_INCOME, DELETE_SAVINGS_INCOME, SELF}
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.outcomes.ResponseWrapper
import v1.models.request.savings.{DeleteRetrieveRawData, DeleteRetrieveRequest}
import v1.models.response.retrieveSavings.RetrieveSavingsHateoasData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveSavingsControllerSpec extends ControllerBaseSpec
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockRetrieveSavingsService
  with MockHateoasFactory
  with MockDeleteRetrieveRequestParser
  with HateoasLinks {

  val nino: String = "AA123456A"
  val taxYear: String = "2017-18"
  val correlationId: String = "X-123"

  val rawData: DeleteRetrieveRawData = DeleteRetrieveRawData(
    nino = nino,
    taxYear = taxYear
  )

  val requestData: DeleteRetrieveRequest = DeleteRetrieveRequest(
    nino = Nino(nino),
    taxYear = DesTaxYear.fromMtd(taxYear)
  )

  val amendSavingsLink: Link =
    Link(
      href = s"/individuals/income-received/savings/$nino/$taxYear",
      method = PUT,
      rel = AMEND_SAVINGS_INCOME
    )

  val retrieveSavingsLink: Link =
    Link(
      href = s"/individuals/income-received/savings/$nino/$taxYear",
      method = GET,
      rel = SELF
    )

  val deleteSavingsLink: Link =
    Link(
      href = s"/individuals/income-received/savings/$nino/$taxYear",
      method = DELETE,
      rel = DELETE_SAVINGS_INCOME
    )

  private val retrieveSavingsResponse = RetrieveSavingsFixture.retrieveSavingsResponseModel
  private val mtdResponse = RetrieveSavingsFixture.mtdResponseWithHateoas(nino, taxYear)

  trait Test {
    val hc = HeaderCarrier()

    val controller = new RetrieveSavingsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      requestParser = mockDeleteRetrieveSavingsRequestParser,
      service = mockRetrieveSavingsService,
      hateoasFactory = mockHateoasFactory,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  "RetrieveSavingsController" should {
    "return OK" when {
      "happy path" in new Test {

        MockDeleteRetrieveSavingsRequestDataParser
          .parse(rawData)
          .returns(Right(requestData))

        MockRetrieveSavingsService
          .retrieveSaving(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, retrieveSavingsResponse))))

        MockHateoasFactory
          .wrap(retrieveSavingsResponse, RetrieveSavingsHateoasData(nino, taxYear))
          .returns(HateoasWrapper(retrieveSavingsResponse,
            Seq(
              amendSavingsLink,
              retrieveSavingsLink,
              deleteSavingsLink
            )
          ))

        val result: Future[Result] = controller.retrieveSaving(nino, taxYear)(fakeGetRequest)

        status(result) shouldBe OK
        contentAsJson(result) shouldBe mtdResponse
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return the error as per spec" when {
      "parser errors occur" must {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockDeleteRetrieveSavingsRequestDataParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

            val result: Future[Result] = controller.retrieveSaving(nino, taxYear)(fakeGetRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)
          }
        }

        val input = Seq(
          (BadRequestError, BAD_REQUEST),
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (RuleTaxYearRangeInvalidError, BAD_REQUEST)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" must {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockDeleteRetrieveSavingsRequestDataParser
              .parse(rawData)
              .returns(Right(requestData))

            MockRetrieveSavingsService
              .retrieveSaving(requestData)
              .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), mtdError))))

            val result: Future[Result] = controller.retrieveSaving(nino, taxYear)(fakeGetRequest)

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
