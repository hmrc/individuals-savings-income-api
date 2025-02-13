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

package v1.createAmendUkSavingsAnnualSummary

import play.api.libs.json.JsValue
import shared.config.SharedAppConfig
import shared.controllers.validators.Validator
import config.SavingsConfig
import v1.createAmendUkSavingsAnnualSummary.def1.Def1_CreateAmendUkSavingsAnnualSummaryValidator
import v1.createAmendUkSavingsAnnualSummary.model.request._

import javax.inject.{Inject, Singleton}

@Singleton
class CreateAmendUkSavingsAnnualSummaryValidatorFactory @Inject() (appConfig: SharedAppConfig, savingsConfig: SavingsConfig) {

  def validator(nino: String, taxYear: String, savingsAccountId: String, body: JsValue): Validator[CreateAmendUkSavingsAnnualSummaryRequestData] =
    new Def1_CreateAmendUkSavingsAnnualSummaryValidator(nino, taxYear, savingsAccountId, body)(appConfig, savingsConfig)

}
