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

package v1.models.response.retrieveOtherEmployment

import config.AppConfig
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OWrites, Reads}
import utils.JsonUtils
import v1.hateoas.{HateoasLinks, HateoasLinksFactory}
import v1.models.hateoas.{HateoasData, Link}

case class RetrieveOtherEmploymentResponse(shareOption: Option[Seq[ShareOptionItem]],
                                           sharesAwardedOrReceived: Option[Seq[SharesAwardedOrReceivedItem]],
                                           disability: Option[CommonOtherEmployment],
                                           foreignService: Option[CommonOtherEmployment])

object RetrieveOtherEmploymentResponse extends HateoasLinks with JsonUtils {

  val empty: RetrieveOtherEmploymentResponse = RetrieveOtherEmploymentResponse(None, None, None, None)

  implicit val reads: Reads[RetrieveOtherEmploymentResponse] = (
    (JsPath \ "shareOption").readNullable[Seq[ShareOptionItem]].mapEmptySeqToNone and
      (JsPath \ "sharesAwardedOrReceived").readNullable[Seq[SharesAwardedOrReceivedItem]].mapEmptySeqToNone and
      (JsPath \ "disability").readNullable[CommonOtherEmployment] and
      (JsPath \ "foreignService").readNullable[CommonOtherEmployment]
    ) (RetrieveOtherEmploymentResponse.apply _)

  implicit val writes: OWrites[RetrieveOtherEmploymentResponse] = Json.writes[RetrieveOtherEmploymentResponse]

  implicit object RetrieveOtherEmploymentLinksFactory extends HateoasLinksFactory[RetrieveOtherEmploymentResponse, RetrieveOtherEmploymentHateoasData] {
    override def links(appConfig: AppConfig, data: RetrieveOtherEmploymentHateoasData): Seq[Link] = {
      import data._
      Seq(
        amendOtherEmployment(appConfig, nino, taxYear),
        retrieveOtherEmployment(appConfig, nino, taxYear),
        deleteOtherEmployment(appConfig, nino, taxYear)
      )
    }
  }

}

case class RetrieveOtherEmploymentHateoasData(nino: String, taxYear: String) extends HateoasData