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
import api.hateoas.HateoasLinks
import api.mocks.MockIdGenerator
import api.mocks.hateoas.MockHateoasFactory
import api.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import api.models.audit.{AuditError, AuditEvent, AuditResponse, FlattenedGenericAuditDetail}
import api.models.auth.UserDetails
import api.models.domain.Nino
import api.models.errors._
import api.models.hateoas.{HateoasWrapper, Link}
import api.models.outcomes.ResponseWrapper
import mocks.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.requestParsers.MockAddUkSavingsAccountRequestParser
import v1.mocks.services.MockAddUkSavingsAccountService
import v1.models.request.addUkSavingsAccount.{AddUkSavingsAccountRawData, AddUkSavingsAccountRequest, AddUkSavingsAccountRequestBody}
import v1.models.response.addUkSavingsAccount.{AddUkSavingsAccountHateoasData, AddUkSavingsAccountResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddUkSavingsAccountControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockAppConfig
    with MockAddUkSavingsAccountService
    with MockAuditService
    with MockAddUkSavingsAccountRequestParser
    with MockHateoasFactory
    with HateoasLinks
    with MockIdGenerator {


  val nino: String = "AA123456A"
  val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val savingsAccountId: String = "SAVKB2UVwUTBQGJ"
  val mtdId: String         = "test-mtd-id"

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new AddUkSavingsAccountController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      requestParser = mockAddUkSavingsAccountRequestParser,
      service = mockAddUkSavingsAccountService,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockedAppConfig.apiGatewayContext.returns("individuals/income-received").anyNumberOfTimes()
    MockIdGenerator.generateCorrelationId.returns(correlationId)

    val links: List[Link] = List(
      listUkSavings(mockAppConfig, nino)
    )

    val requestBodyJson: JsValue = Json.parse(
      """
        |{
        |   "accountName": "Shares savings account"
        |}
        |""".stripMargin)

    val rawData: AddUkSavingsAccountRawData = AddUkSavingsAccountRawData(
      nino = nino,
      body = AnyContentAsJson(requestBodyJson)
    )

    val addUkSavingsAccountRequestBody: AddUkSavingsAccountRequestBody = AddUkSavingsAccountRequestBody(
      "Shares savings account"
    )

    val requestData: AddUkSavingsAccountRequest = AddUkSavingsAccountRequest(
      nino = Nino(nino),
      body = AddUkSavingsAccountRequestBody("Shares savings account")
    )

    val responseData: AddUkSavingsAccountResponse = AddUkSavingsAccountResponse(
      savingsAccountId = savingsAccountId
    )

    val responseJson: JsValue = Json.parse(
      s"""
         |{
         |    "savingsAccountId": "$savingsAccountId",
         |    "links":[
         |      {
         |         "href":"/individuals/income-received/savings/uk-accounts/$nino",
         |         "method":"GET",
         |         "rel":"list-all-uk-savings-account"
         |      }
         |   ]
         |}
         |""".stripMargin)

    def event(auditResponse: AuditResponse): AuditEvent[FlattenedGenericAuditDetail] =
      AuditEvent(
        auditType = "AddUkSavingsAccount",
        transactionName = "add-uk-savings-account",
        detail = FlattenedGenericAuditDetail(
          versionNumber = Some("1.0"),
          userDetails = UserDetails(mtdId, "Individual", None),
          params = Map("nino" -> nino),
          request = Some(requestBodyJson),
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )
  }

  "AddUkSavingsAccountController" should {
    "return OK" when {
      "happy path" in new Test {

        MockAddUkSavingsAccountRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockAddUkSavingsAccountService
          .addUkSavingsAccountService(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseData))))

        MockHateoasFactory
          .wrap(responseData, AddUkSavingsAccountHateoasData(nino, savingsAccountId))
          .returns(HateoasWrapper(responseData, links))

        val result: Future[Result] = controller.addUkSavingsAccount(nino)(fakePostRequest(requestBodyJson))

        status(result) shouldBe OK
        contentAsJson(result) shouldBe responseJson
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(OK, None, Some(Json.toJson(responseData)))
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }
    "return the error as per spec" when {
      "parser errors occur" must {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockAddUkSavingsAccountRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(correlationId, error, None)))

            val result: Future[Result] = controller.addUkSavingsAccount(nino)(fakePostRequest(requestBodyJson))

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
          (AccountNameFormatError, BAD_REQUEST)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" must {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockAddUkSavingsAccountRequestParser
              .parse(rawData)
              .returns(Right(requestData))

            MockAddUkSavingsAccountService
              .addUkSavingsAccountService(requestData)
              .returns(Future.successful(Left(ErrorWrapper(correlationId, mtdError))))

            val result: Future[Result] = controller.addUkSavingsAccount(nino)(fakePostRequest(requestBodyJson))

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(mtdError.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once
          }
        }

        val input = Seq(
          (NinoFormatError, BAD_REQUEST),
          (RuleMaximumSavingsAccountsLimitError, FORBIDDEN),
          (RuleDuplicateAccountNameError, FORBIDDEN),
          (StandardDownstreamError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }
}
