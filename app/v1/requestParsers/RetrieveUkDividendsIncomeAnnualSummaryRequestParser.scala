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

package v1.requestParsers

import api.models.domain.{Nino, TaxYear}
import api.requestParsers.RequestParser
import v1.models.request.retrieveUkDividendsAnnualIncomeSummary.{
  RetrieveUkDividendsAnnualIncomeSummaryRawData,
  RetrieveUkDividendsAnnualIncomeSummaryRequest
}
import v1.requestParsers.validators.RetrieveUkDividendsIncomeAnnualSummaryValidator

import javax.inject.{Inject, Singleton}

@Singleton
class RetrieveUkDividendsIncomeAnnualSummaryRequestParser @Inject() (val validator: RetrieveUkDividendsIncomeAnnualSummaryValidator)
    extends RequestParser[RetrieveUkDividendsAnnualIncomeSummaryRawData, RetrieveUkDividendsAnnualIncomeSummaryRequest] {

  override protected def requestFor(data: RetrieveUkDividendsAnnualIncomeSummaryRawData): RetrieveUkDividendsAnnualIncomeSummaryRequest =
    RetrieveUkDividendsAnnualIncomeSummaryRequest(Nino(data.nino), TaxYear.fromMtd(data.taxYear))

}