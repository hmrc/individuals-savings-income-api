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

import api.models.errors.{MtdError, RuleDisposalDateError}
import api.models.errors.RuleDisposalDateError

import java.time.LocalDate

object DisposalDateValidation {

  def validate(date: String, taxYear: String, path: String, validateToday: Boolean, errorMessage: String): List[MtdError] = {
    val now        = LocalDate.now()
    val parsedDate = LocalDate.parse(date)
    if (validateToday && parsedDate.isAfter(now)) {
      List(RuleDisposalDateError.copy(paths = Some(Seq(path)), message = errorMessage))
    } else {
      val (fromDate, toDate) = getToDateAndFromDate(taxYear)

      if (parsedDate.isBefore(fromDate) || parsedDate.isAfter(toDate)) {
        List(RuleDisposalDateError.copy(paths = Some(Seq(path)), message = errorMessage))
      } else {
        NoValidationErrors
      }
    }
  }

}
