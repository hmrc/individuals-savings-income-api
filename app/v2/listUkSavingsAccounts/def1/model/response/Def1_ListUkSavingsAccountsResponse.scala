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

package v2.listUkSavingsAccounts.def1.model.response

import play.api.libs.json._
import utils.JsonUtils
import v2.listUkSavingsAccounts.model.response.ListUkSavingsAccountsResponse

case class Def1_ListUkSavingsAccountsResponse[E](savingsAccounts: Option[Seq[E]]) extends ListUkSavingsAccountsResponse[E]

object Def1_ListUkSavingsAccountsResponse extends JsonUtils {

  implicit def writes[E: Writes]: OWrites[Def1_ListUkSavingsAccountsResponse[E]] = Json.writes[Def1_ListUkSavingsAccountsResponse[E]]

  implicit def reads[E: Reads]: Reads[Def1_ListUkSavingsAccountsResponse[E]] = {
    case JsObject(fields) if fields.size == 1 && fields.contains("bbsi") =>
      fields.get("bbsi").map(arr =>
        arr.validate(JsPath
          .readNullable[Seq[E]]
          .mapEmptySeqToNone
          .map(Def1_ListUkSavingsAccountsResponse(_))
        )
      ).getOrElse(JsError("Unexpected JSON format"))

    case _ =>
      JsError("Unexpected JSON format")
  }

}
