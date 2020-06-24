/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.models.request.amendForeign

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OWrites, Reads}
import utils.JsonUtils

case class AmendForeignRequestBody(foreignEarnings: Option[ForeignEarnings],
                                   unremittableForeignIncome: Option[Seq[UnremittableForeignIncomeItem]])

object AmendForeignRequestBody extends JsonUtils {
  val empty: AmendForeignRequestBody = AmendForeignRequestBody(None, None)

  implicit val reads: Reads[AmendForeignRequestBody] = (
    (JsPath \ "foreignEarnings").readNullable[ForeignEarnings].map(_.flatMap {
      case ForeignEarnings.empty => None
      case foreignEarnings => Some(foreignEarnings)
    }) and
      (JsPath \ "unremittableForeignIncome").readNullable[Seq[UnremittableForeignIncomeItem]].mapEmptySeqToNone
    ) (AmendForeignRequestBody.apply _)

  implicit val writes: OWrites[AmendForeignRequestBody] = (
    (JsPath \ "foreignEarnings").writeNullable[ForeignEarnings] and
      (JsPath \ "unremittableForeignIncome").writeNullable[Seq[UnremittableForeignIncomeItem]]
    ) (unlift(AmendForeignRequestBody.unapply))
}