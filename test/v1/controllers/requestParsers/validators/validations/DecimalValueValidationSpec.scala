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

package v1.controllers.requestParsers.validators.validations

import support.UnitSpec
import v1.models.errors.ValueFormatError

class DecimalValueValidationSpec extends UnitSpec with ValueFormatErrorMessages {

  "DecimalValueValidation" when {
    "validate" must {
      "return an empty list for a valid decimal value (default validation)" in {
        DecimalValueValidation.validate(
          amount = 100,
          path = "/path"
        ) shouldBe NoValidationErrors
      }

      "return an empty list for a valid decimal value (custom validation)" in {
        DecimalValueValidation.validate(
          amount = 100,
          maxScale = 1,
          minValue = 99,
          maxValue = 101,
          path = "/path"
        ) shouldBe NoValidationErrors
      }

      "return a ValueFormatError for an invalid decimal value (default validation)" in {
        DecimalValueValidation.validate(
          amount = -100,
          path = "/path"
        ) shouldBe List(ValueFormatError.copy(message = ZERO_MINIMUM_INCLUSIVE, paths = Some(Seq("/path"))))
      }

      "return a ValueFormatError for an invalid decimal value (custom validation)" in {
        DecimalValueValidation.validate(
          amount = 98,
          maxScale = 0,
          minValue = 99,
          maxValue = 101,
          path = "/path",
          message = "error message"
        ) shouldBe List(ValueFormatError.copy(message = "error message", paths = Some(Seq("/path"))))
      }
    }

    "validateOptional" should {
      "return an empty list for a value of 'None'" in {
        DecimalValueValidation.validateOptional(
          amount = None,
          path = "/path"
        ) shouldBe NoValidationErrors
      }

      "validate correctly for some valid amount" in {
        DecimalValueValidation.validateOptional(
          amount = Some(100),
          path = "/path"
        ) shouldBe NoValidationErrors
      }

      "validate correctly for some invalid amount" in {
        DecimalValueValidation.validateOptional(
          amount = Some(-100),
          path = "/path"
        ) shouldBe List(ValueFormatError.copy(message = ZERO_MINIMUM_INCLUSIVE, paths = Some(Seq("/path"))))
      }
    }
  }
}