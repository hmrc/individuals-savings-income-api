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

import api.models.errors.PayrollIdFormatError
import support.UnitSpec

class PayrollIdValidationSpec extends UnitSpec {

  "PayrollIdValidation" when {
    "validate" should {
      "return an empty list for a valid payroll ID" in {
        PayrollIdValidation.validate("124214112412") shouldBe NoValidationErrors
      }

      "return an PayrollIdFormatError error for an invalid payroll ID" in {
        PayrollIdValidation.validate("££££") shouldBe List(PayrollIdFormatError)
      }
    }

    "validateOptional" should {
      "return an empty list when no payroll ID is supplied" in {
        PayrollIdValidation.validateOptional(None) shouldBe NoValidationErrors
      }

      "return an empty list when a valid payroll ID is supplied" in {
        PayrollIdValidation.validateOptional(Some("124214112412")) shouldBe NoValidationErrors
      }

      "return an PayrollIdFormatError error when an invalid payroll ID supplied" in {
        PayrollIdValidation.validateOptional(Some("£££")) shouldBe List(PayrollIdFormatError)
      }
    }
  }

}
