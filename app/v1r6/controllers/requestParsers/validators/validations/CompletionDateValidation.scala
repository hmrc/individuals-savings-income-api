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

package v1r6.controllers.requestParsers.validators.validations

import utils.CurrentDateTime
import v1r6.models.domain.DesTaxYear
import v1r6.models.errors.{MtdError, RuleCompletionDateError}

import java.time.LocalDate

object CompletionDateValidation {
  private val MARCH = 3
  private val SEVEN = 7

  def validate(date: String, path: String, taxYear: String)(implicit currentDateTime: CurrentDateTime): List[MtdError] = {
    val formattedDate = LocalDate.parse(date)
    val march7th = LocalDate.parse(DesTaxYear.fromMtd(taxYear).value, yearFormat).withMonth(MARCH).withDayOfMonth(SEVEN)

    val (fromDate, toDate) = getToDateAndFromDate(taxYear)

    val dateIsBefore7thMarch = formattedDate.isBefore(march7th)
    val dateIsAfterToday = formattedDate.isAfter(currentDateTime.getLocalDate)
    val dateIsInTaxYear = formattedDate.isAfter(fromDate) && formattedDate.isBefore(toDate)

    if(dateIsBefore7thMarch || dateIsAfterToday || !dateIsInTaxYear) {
      List(RuleCompletionDateError.copy(paths = Some(Seq(path))))
    } else {
      NoValidationErrors
    }
  }
}