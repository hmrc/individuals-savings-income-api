/*
 * Copyright 2025 HM Revenue & Customs
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

package v2.listUkSavingsAccounts.model.response

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

case class UkSavingsAccount(savingsAccountId: String, accountName: Option[String])

object UkSavingsAccount {

  implicit val writes: OWrites[UkSavingsAccount] = Json.writes[UkSavingsAccount]

  implicit val reads: Reads[UkSavingsAccount] = (
    (JsPath \ "incomeSourceId").read[String] and
      (JsPath \ "incomeSourceName").readNullable[String]
  )(UkSavingsAccount.apply)

}
