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

import api.models.errors.{MtdError, PayrollIdFormatError}

object PayrollIdValidation {

  def validateOptional(payrollId: Option[String]): List[MtdError] = payrollId match {
    case None => NoValidationErrors
    case Some(value) => validate(value)
  }

  def validate(payrollId: String): List[MtdError] = {
    val regex = "^[A-Za-z0-9.,\\-()/=!\"%&*; <>'+:\\?]{0,38}$"
    if (payrollId.matches(regex)) NoValidationErrors else List(PayrollIdFormatError)
  }
}