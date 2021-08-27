/*
 * Copyright 2021 HM Revenue & Customs
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

package v1r6.models.response.retrieveOtherCgt

import config.AppConfig
import play.api.libs.json.{Json, OFormat}
import v1r6.hateoas.{HateoasLinks, HateoasLinksFactory}
import v1r6.models.hateoas.{HateoasData, Link}

case class RetrieveOtherCgtResponse(submittedOn: String,
                                    disposals: Option[Seq[Disposal]],
                                    nonStandardGains: Option[NonStandardGains],
                                    losses: Option[Losses],
                                    adjustments: Option[BigDecimal])

object RetrieveOtherCgtResponse extends HateoasLinks {
  implicit val format: OFormat[RetrieveOtherCgtResponse] = Json.format[RetrieveOtherCgtResponse]

  implicit object RetrieveOtherCgtLinksFactory extends HateoasLinksFactory[RetrieveOtherCgtResponse, RetrieveOtherCgtHateoasData]{
    override def links(appConfig: AppConfig, data: RetrieveOtherCgtHateoasData): Seq[Link] = {
      Seq(
        createAmendOtherCgt(appConfig, data.nino, data.taxYear),
        deleteOtherCgt(appConfig, data.nino, data.taxYear),
        retrieveOtherCgt(appConfig, data.nino, data.taxYear)
      )
    }
  }
}

case class RetrieveOtherCgtHateoasData(nino: String, taxYear: String) extends HateoasData