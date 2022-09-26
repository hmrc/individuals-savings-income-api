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
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v1.mocks.connectors.MockRetrieveUKDividendsIncomeAnnualSummaryConnector
import v1.models.request.retrieveUkDividendsAnnualIncomeSummary.RetrieveUkDividendsAnnualIncomeSummaryRequest
import v1.models.response.retrieveUkDividendsAnnualIncomeSummary.RetrieveUkDividendsAnnualIncomeSummaryResponse

import scala.concurrent.Future

class RetrieveUkDividendsIncomeAnnualSummaryServiceSpec extends ServiceSpec {

  private val nino    = "AA112233A"
  private val taxYear = "2019-20"

  private val requestData = RetrieveUkDividendsAnnualIncomeSummaryRequest(Nino(nino), TaxYear.fromMtd(taxYear))

  private val validResponse = RetrieveUkDividendsAnnualIncomeSummaryResponse(
    ukDividends = Some(10.12),
    otherUkDividends = Some(11.12)
  )

  trait Test extends MockRetrieveUKDividendsIncomeAnnualSummaryConnector {
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    val service: RetrieveUkDividendsIncomeAnnualSummaryService =
      new RetrieveUkDividendsIncomeAnnualSummaryService(connector = mockRetrieveUKDividendsIncomeAnnualSummaryConnector)

  }

  "RetrieveUKDividendsIncomeAnnualSummaryService" when {
    "retrieveUKDividendsIncomeAnnualSummary" must {
      "return correct result for a success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, validResponse))

        MockRetrieveUKDividendsIncomeAnnualSummaryConnector
          .retrieveUKDividendsIncomeAnnualSummary(requestData)
          .returns(Future.successful(outcome))

        await(service.retrieveUKDividendsIncomeAnnualSummary(requestData)) shouldBe outcome
      }

      "map errors according to spec" when {

        def serviceError(desErrorCode: String, error: MtdError): Unit =
          s"a $desErrorCode error is returned from the service" in new Test {

            MockRetrieveUKDividendsIncomeAnnualSummaryConnector
              .retrieveUKDividendsIncomeAnnualSummary(requestData)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(desErrorCode))))))

            await(service.retrieveUKDividendsIncomeAnnualSummary(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val input = Seq(
          ("INVALID_NINO", NinoFormatError),
          ("INVALID_TYPE", StandardDownstreamError),
          ("INVALID_TAXYEAR", TaxYearFormatError),
          ("INVALID_INCOME_SOURCE", StandardDownstreamError),
          ("NOT_FOUND_PERIOD", NotFoundError),
          ("NOT_FOUND_INCOME_SOURCE", NotFoundError),
          ("SERVER_ERROR", StandardDownstreamError),
          ("SERVICE_UNAVAILABLE", StandardDownstreamError)
        )

        val extraTysErrors = Seq(
          ("INVALID_TAX_YEAR", TaxYearFormatError),
          ("INVALID_INCOMESOURCE_ID", StandardDownstreamError),
          ("INVALID_INCOMESOURCE_TYPE", StandardDownstreamError),
          ("INVALID_CORRELATION_ID", StandardDownstreamError),
          ("SUBMISSION_PERIOD_NOT_FOUND", NotFoundError),
          ("INCOME_DATA_SOURCE_NOT_FOUND", NotFoundError),
          ("TAX_YEAR_NOT_SUPPORTED", RuleTaxYearNotSupportedError)
        )
        val fullErrorList = input ++ extraTysErrors

        fullErrorList.foreach(args => (serviceError _).tupled(args))
      }
    }
  }

}
