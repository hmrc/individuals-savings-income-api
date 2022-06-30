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

package v1.models.request.createAmendUkSavingsAnnualSummary

import api.models.domain.{Nino, TaxYear}
import api.models.request.RawData
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.AnyContentAsJson

case class CreateAmendUkSavingsAnnualSummaryRawData(nino: String, taxYear: String, savingsAccountId: String, body: AnyContentAsJson) extends RawData

case class CreateAmendUkSavingsAnnualSummaryBody(taxedUkInterest: Option[BigDecimal], untaxedUkInterest: Option[BigDecimal])

object CreateAmendUkSavingsAnnualSummaryBody {
  implicit val format: OFormat[CreateAmendUkSavingsAnnualSummaryBody] = Json.format[CreateAmendUkSavingsAnnualSummaryBody]
}

case class CreateAmendUkDividendsIncomeAnnualSummaryRequest(nino: Nino,
                                                            taxYear: TaxYear,
                                                            savingsAccountId: String,
                                                            body: CreateAmendUkSavingsAnnualSummaryBody)