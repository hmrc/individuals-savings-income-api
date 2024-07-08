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

package v1.createAmendUkSavingsAnnualSummary.def1.model.request

import models.domain.SavingsAccountId
import shared.models.domain.{Nino, TaxYear}
import v1.createAmendUkSavingsAnnualSummary.CreateAmendUkSavingsAnnualSummarySchema
import v1.createAmendUkSavingsAnnualSummary.CreateAmendUkSavingsAnnualSummarySchema.Def1
import v1.createAmendUkSavingsAnnualSummary.model.request.CreateAmendUkSavingsAnnualSummaryRequestData

case class Def1_CreateAmendUkSavingsAnnualSummaryRequestData(
    nino: Nino,
    taxYear: TaxYear,
    savingsAccountId: SavingsAccountId,
    mtdBody: Def1_CreateAmendUkSavingsAnnualSummaryRequestBody
) extends CreateAmendUkSavingsAnnualSummaryRequestData {

  override val schema: CreateAmendUkSavingsAnnualSummarySchema = Def1
}
