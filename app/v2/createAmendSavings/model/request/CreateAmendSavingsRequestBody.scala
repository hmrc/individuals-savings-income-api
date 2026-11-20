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

package v2.createAmendSavings.model.request

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*

case class CreateAmendSavingsRequestBody(securities: Option[AmendSecurities], foreignInterest: Option[Seq[AmendForeignInterestItem]])

object CreateAmendSavingsRequestBody {
  val empty: CreateAmendSavingsRequestBody = CreateAmendSavingsRequestBody(None, None)

  implicit val reads: Reads[CreateAmendSavingsRequestBody] = (
    (JsPath \ "securities").readNullable[AmendSecurities] and
      (JsPath \ "foreignInterest").readNullable[Seq[AmendForeignInterestItem]]
  )(CreateAmendSavingsRequestBody.apply)

  implicit val writes: OWrites[CreateAmendSavingsRequestBody] = (
    (JsPath \ "securities").writeNullable[AmendSecurities] and
      (JsPath \ "foreignInterest").writeNullable[Seq[AmendForeignInterestItem]]
  )(w => Tuple.fromProductTyped(w))

}
