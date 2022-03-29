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

package v1.controllers

import api.controllers.ControllerBaseSpec
import api.mocks.MockIdGenerator
import api.mocks.requestParsers.MockDeleteRetrieveRequestParser
import api.mocks.services.{ MockAuditService, MockDeleteRetrieveService, MockEnrolmentsAuthService, MockMtdIdLookupService }
import api.models.audit.{ AuditError, AuditEvent, AuditResponse, GenericAuditDetail }
import api.models.domain.Nino
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.models.request.{ DeleteRetrieveRawData, DeleteRetrieveRequest }
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteNonPayeEmploymentControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockDeleteRetrieveRequestParser
    with MockDeleteRetrieveService
    with MockAuditService
    with MockIdGenerator {

  val nino: String          = "AC203948B"
  val taxYear: String       = "2020-21"
  val correlationId: String = "a1e8057e-fbbc-47a8-a8b478d9f0123456"

  def event(auditResponse: AuditResponse): AuditEvent[GenericAuditDetail] =
    AuditEvent(
      auditType = "DeleteNonPayeEmploymentIncome",
      transactionName = "delete-non-paye-employment-income",
      detail = GenericAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        params = Map("nino" -> nino, "taxYear" -> taxYear),
        None,
        correlationId,
        response = auditResponse
      )
    )

  val rawData: DeleteRetrieveRawData = DeleteRetrieveRawData(
    nino = nino,
    taxYear = taxYear
  )

  val requestData: DeleteRetrieveRequest = DeleteRetrieveRequest(
    nino = Nino(nino),
    taxYear = taxYear
  )

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new DeleteNonPayeEmploymentController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      requestParser = mockDeleteRetrieveRequestParser,
      service = mockDeleteRetrieveService,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockIdGenerator.generateCorrelationId.returns(correlationId)
  }

  "DeleteNonPayeEmploymentController" when {
    "delete" should {
      "return a 204 NO_CONTENT" in new Test {

        MockDeleteRetrieveRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockDeleteRetrieveService
          .delete(defaultDownstreamErrorMap)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        val result: Future[Result] = controller.delete(nino, taxYear)(fakeDeleteRequest)

        status(result) shouldBe NO_CONTENT
        contentAsString(result) shouldBe ""
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(NO_CONTENT, None, None)
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }

      "return the error as per spec" when {
        "parser errors occur" must {
          def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
            s"a ${error.code} error is returned from the parser" in new Test {

              MockDeleteRetrieveRequestParser
                .parse(rawData)
                .returns(Left(ErrorWrapper(correlationId, error, None)))

              val result: Future[Result] = controller.delete(nino, taxYear)(fakeDeleteRequest)

              status(result) shouldBe expectedStatus
              contentAsJson(result) shouldBe Json.toJson(error)
              header("X-CorrelationId", result) shouldBe Some(correlationId)

              val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None)
              MockedAuditService.verifyAuditEvent(event(auditResponse)).once
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
                .delete(defaultDownstreamErrorMap)
                .returns(Future.successful(Left(ErrorWrapper(correlationId, mtdError))))

              val result: Future[Result] = controller.delete(nino, taxYear)(fakeDeleteRequest)

              status(result) shouldBe expectedStatus
              contentAsJson(result) shouldBe Json.toJson(mtdError)
              header("X-CorrelationId", result) shouldBe Some(correlationId)

              val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(mtdError.code))), None)
              MockedAuditService.verifyAuditEvent(event(auditResponse)).once
            }
          }

          val input = Seq(
            (NinoFormatError, BAD_REQUEST),
            (TaxYearFormatError, BAD_REQUEST),
            (NotFoundError, NOT_FOUND),
            (StandardDownstreamError, INTERNAL_SERVER_ERROR)
          )

          input.foreach(args => (serviceErrors _).tupled(args))
        }
      }
    }
  }
}