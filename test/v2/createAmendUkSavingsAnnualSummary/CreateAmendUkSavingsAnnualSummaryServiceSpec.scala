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

package v2.createAmendUkSavingsAnnualSummary

import models.domain.SavingsAccountId
import models.errors.RuleOutsideAmendmentWindowError
import shared.controllers.EndpointLogContext
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v2.createAmendUkSavingsAnnualSummary.def1.model.request.{
  Def1_CreateAmendUkSavingsAnnualSummaryRequestBody,
  Def1_CreateAmendUkSavingsAnnualSummaryRequestData
}

import scala.concurrent.Future

class CreateAmendUkSavingsAnnualSummaryServiceSpec extends ServiceSpec {

  private val nino                     = Nino("AA112233A")
  private val taxYear                  = TaxYear.fromMtd("2019-20")
  private val savingsAccountId: String = "ABC1234567890"

  private val requestData = Def1_CreateAmendUkSavingsAnnualSummaryRequestData(
    nino = nino,
    taxYear = taxYear,
    SavingsAccountId(savingsAccountId),
    mtdBody = Def1_CreateAmendUkSavingsAnnualSummaryRequestBody(None, None)
  )

  trait Test extends MockCreateAmendUkSavingsAnnualSummaryConnector {
    implicit val logContext: EndpointLogContext = EndpointLogContext("Savings", "amend")

    val service: CreateAmendUkSavingsAnnualSummaryService = new CreateAmendUkSavingsAnnualSummaryService(
      connector = mockAmendUkSavingsConnector
    )

  }

  "CreateAmendUkSavingsAnnualSummaryService" when {
    "the downstream request is successful" must {
      "return a success result" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockCreateAmendUkSavingsAnnualSummaryConnector
          .createOrAmendAnnualSummary(requestData)
          .returns(Future.successful(outcome))

        await(service.createAmend(requestData)) shouldBe outcome
      }

      "map errors according to spec" when {

        def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
          s"a $downstreamErrorCode error is returned from the service" in new Test {

            MockCreateAmendUkSavingsAnnualSummaryConnector
              .createOrAmendAnnualSummary(requestData)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

            await(service.createAmend(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val errors = List(
          ("INVALID_NINO", NinoFormatError),
          ("INVALID_TAXYEAR", TaxYearFormatError), // remove once DES to IFS migration complete
          ("INVALID_TYPE", InternalError),
          ("INVALID_PAYLOAD", InternalError),
          ("NOT_FOUND_INCOME_SOURCE", NotFoundError),
          ("MISSING_CHARITIES_NAME_GIFT_AID", InternalError),
          ("MISSING_GIFT_AID_AMOUNT", InternalError),
          ("MISSING_CHARITIES_NAME_INVESTMENT", InternalError),
          ("MISSING_INVESTMENT_AMOUNT", InternalError),
          ("INVALID_ACCOUNTING_PERIOD", RuleTaxYearNotSupportedError),
          ("GONE", InternalError),
          ("NOT_FOUND", NotFoundError),
          ("SERVER_ERROR", InternalError),
          ("SERVICE_UNAVAILABLE", InternalError)
        )
        val tysErrors = List(
          "INVALID_TAX_YEAR"           -> TaxYearFormatError,
          "INCOME_SOURCE_NOT_FOUND"    -> NotFoundError,
          "INVALID_INCOMESOURCE_TYPE"  -> InternalError,
          "INVALID_CORRELATIONID"      -> InternalError,
          "INCOMPATIBLE_INCOME_SOURCE" -> InternalError,
          "TAX_YEAR_NOT_SUPPORTED"     -> RuleTaxYearNotSupportedError,
          "OUTSIDE_AMENDMENT_WINDOW"   -> RuleOutsideAmendmentWindowError
        )

        (errors ++ tysErrors).foreach(args => (serviceError _).tupled(args))
      }
    }
  }

}
