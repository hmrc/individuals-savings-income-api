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

package v1.services

import api.controllers.EndpointLogContext
import api.models.domain.{TaxYear, Nino}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v1.mocks.connectors.MockCreateAmendUkDividendsAnnualSummaryConnector
import v1.models.request.createAmendUkDividendsIncomeAnnualSummary.{
  CreateAmendUkDividendsIncomeAnnualSummaryBody,
  CreateAmendUkDividendsIncomeAnnualSummaryRequest
}

import scala.concurrent.Future

class CreateAmendAmendUkDividendsAnnualSummaryServiceSpec extends ServiceSpec {

  private val request = CreateAmendUkDividendsIncomeAnnualSummaryRequest(
    nino = Nino("AA112233A"),
    taxYear = TaxYear.fromMtd("2019-20"),
    body = CreateAmendUkDividendsIncomeAnnualSummaryBody(None, None)
  )

  trait Test extends MockCreateAmendUkDividendsAnnualSummaryConnector {
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    val service: CreateAmendAmendUkDividendsAnnualSummaryService = new CreateAmendAmendUkDividendsAnnualSummaryService(
      connector = mockAmendUkDividendsConnector
    )

  }

  "CreateAmendAmendUkDividendsAnnualSummaryService" when {
    "the downstream request is successful" must {
      "return a success result" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockCreateAmendUkDividendsAnnualSummaryConnector
          .createOrAmendAnnualSummary(request)
          .returns(Future.successful(outcome))

        await(service.createOrAmendAnnualSummary(request)) shouldBe outcome
      }

      "map errors according to spec" when {

        def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
          s"a $downstreamErrorCode error is returned from the service" in new Test {

            MockCreateAmendUkDividendsAnnualSummaryConnector
              .createOrAmendAnnualSummary(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

            await(service.createOrAmendAnnualSummary(request)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val input = Seq(
          ("INVALID_NINO", NinoFormatError),
          ("INVALID_TAXYEAR", TaxYearFormatError),
          ("INVALID_TYPE", StandardDownstreamError),
          ("INVALID_PAYLOAD", StandardDownstreamError),
          ("NOT_FOUND_INCOME_SOURCE", NotFoundError),
          ("MISSING_CHARITIES_NAME_GIFT_AID", StandardDownstreamError),
          ("MISSING_GIFT_AID_AMOUNT", StandardDownstreamError),
          ("MISSING_CHARITIES_NAME_INVESTMENT", StandardDownstreamError),
          ("MISSING_INVESTMENT_AMOUNT", StandardDownstreamError),
          ("INVALID_ACCOUNTING_PERIOD", RuleTaxYearNotSupportedError),
          ("GONE", StandardDownstreamError),
          ("NOT_FOUND", NotFoundError),
          ("SERVER_ERROR", StandardDownstreamError),
          ("SERVICE_UNAVAILABLE", StandardDownstreamError)
        )

        input.foreach(args => (serviceError _).tupled(args))
      }
    }
  }

}