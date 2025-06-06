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

package v2.deleteSavings.def1

import cats.data.Validated
import cats.implicits.catsSyntaxTuple2Semigroupal
import shared.config.SharedAppConfig
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveNino, ResolveTaxYearMinimum}
import shared.models.domain.TaxYear
import shared.models.errors.MtdError
import config.SavingsConfig
import v2.deleteSavings.def1.model.request.Def1_DeleteSavingsRequestData
import v2.deleteSavings.model.request.DeleteSavingsRequestData

class Def1_DeleteSavingsValidator(nino: String, taxYear: String)(appConfig: SharedAppConfig, savingsConfig: SavingsConfig)
    extends Validator[DeleteSavingsRequestData] {

  private lazy val minimumTaxYear = savingsConfig.minimumPermittedTaxYear
  private lazy val resolveTaxYear = ResolveTaxYearMinimum(TaxYear.fromDownstreamInt(minimumTaxYear))

  def validate: Validated[Seq[MtdError], DeleteSavingsRequestData] = (ResolveNino(nino), resolveTaxYear(taxYear))
    .mapN(Def1_DeleteSavingsRequestData)

}
