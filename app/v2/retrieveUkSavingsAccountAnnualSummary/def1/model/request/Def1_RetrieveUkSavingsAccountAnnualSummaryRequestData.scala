/*
 * Copyright 2024 HM Revenue & Customs
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

package v2.retrieveUkSavingsAccountAnnualSummary.def1.model.request

import models.domain.SavingsAccountId
import shared.models.domain.{Nino, TaxYear}
import v2.retrieveUkSavingsAccountAnnualSummary.RetrieveUkSavingsAccountAnnualSummarySchema
import v2.retrieveUkSavingsAccountAnnualSummary.RetrieveUkSavingsAccountAnnualSummarySchema.Def1
import v2.retrieveUkSavingsAccountAnnualSummary.model.request.RetrieveUkSavingsAccountAnnualSummaryRequestData

case class Def1_RetrieveUkSavingsAccountAnnualSummaryRequestData(nino: Nino, taxYear: TaxYear, savingsAccountId: SavingsAccountId)
    extends RetrieveUkSavingsAccountAnnualSummaryRequestData {
  val schema: RetrieveUkSavingsAccountAnnualSummarySchema = Def1
}
