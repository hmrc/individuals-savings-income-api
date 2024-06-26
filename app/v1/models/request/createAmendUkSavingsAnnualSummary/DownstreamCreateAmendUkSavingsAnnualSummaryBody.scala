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

package v1.models.request.createAmendUkSavingsAnnualSummary

import play.api.libs.json.{Json, Writes}

case class DownstreamCreateAmendUkSavingsAnnualSummaryBody(incomeSourceId: String,
                                                           taxedUkInterest: Option[BigDecimal],
                                                           untaxedUkInterest: Option[BigDecimal])

object DownstreamCreateAmendUkSavingsAnnualSummaryBody {
  implicit val writes: Writes[DownstreamCreateAmendUkSavingsAnnualSummaryBody] = Json.writes

  def apply(mtdRequest: CreateAmendUkSavingsAnnualSummaryRequestData): DownstreamCreateAmendUkSavingsAnnualSummaryBody =
    DownstreamCreateAmendUkSavingsAnnualSummaryBody(
      incomeSourceId = mtdRequest.savingsAccountId.toString,
      taxedUkInterest = mtdRequest.body.taxedUkInterest,
      untaxedUkInterest = mtdRequest.body.untaxedUkInterest)

}
