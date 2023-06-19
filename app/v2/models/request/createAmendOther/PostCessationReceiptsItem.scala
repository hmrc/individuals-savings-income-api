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

package v2.models.request.createAmendOther

import play.api.libs.json.{Format, Json}

case class PostCessationReceiptsItem(customerReference: Option[String],
                                     businessName: Option[String],
                                     dateBusinessCeased: Option[String],
                                     businessDescription: Option[String],
                                     incomeSource: Option[String],
                                     amount: BigDecimal,
                                     taxYearIncomeToBeTaxed: String)

object PostCessationReceiptsItem {

  implicit val format: Format[PostCessationReceiptsItem] = Json.format[PostCessationReceiptsItem]

}