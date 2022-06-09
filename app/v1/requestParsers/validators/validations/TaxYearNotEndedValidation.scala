/*
 * Copyright 2022 HM Revenue & Customs
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

package v1.requestParsers.validators.validations

import api.models.domain.TaxYear
import api.models.errors.{MtdError, RuleTaxYearNotEndedError}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import utils.CurrentDateTime
import api.models.errors.RuleTaxYearNotEndedError

object TaxYearNotEndedValidation {

  // @param taxYear In format YYYY-YY
  def validate(taxYear: String)(implicit dateTimeProvider: CurrentDateTime): List[MtdError] = {

    val downstreamTaxYear     = Integer.parseInt(TaxYear.fromMtd(taxYear).toDownstream)
    val currentDate: DateTime = dateTimeProvider.getDateTime

    if (downstreamTaxYear >= getCurrentTaxYear(currentDate)) List(RuleTaxYearNotEndedError)
    else NoValidationErrors
  }

  private def getCurrentTaxYear(date: DateTime): Int = {

    lazy val taxYearStartDate: DateTime = DateTime.parse(
      date.getYear + "-04-06",
      DateTimeFormat.forPattern("yyyy-MM-dd")
    )

    if (date.isBefore(taxYearStartDate)) date.getYear else date.getYear + 1
  }

}
