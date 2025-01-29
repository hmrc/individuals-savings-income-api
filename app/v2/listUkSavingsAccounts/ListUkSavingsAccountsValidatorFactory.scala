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

package v2.listUkSavingsAccounts

import shared.controllers.validators.Validator
import v2.listUkSavingsAccounts.def1.Def1_ListUkSavingsAccountsValidator
import v2.listUkSavingsAccounts.model.request.ListUkSavingsAccountsRequestData

import javax.inject.Singleton

@Singleton
class ListUkSavingsAccountsValidatorFactory {

  def validator(nino: String, savingsAccountId: Option[String]): Validator[ListUkSavingsAccountsRequestData] =
    new Def1_ListUkSavingsAccountsValidator(nino, savingsAccountId)

}
