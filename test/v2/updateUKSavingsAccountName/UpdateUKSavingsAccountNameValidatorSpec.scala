/*
 * Copyright 2025 HM Revenue & Customs
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

package v2.updateUKSavingsAccountName

import play.api.libs.json._
import shared.models.domain.Nino
import shared.models.errors._
import shared.models.utils.JsonErrorValidators
import shared.utils.UnitSpec
import v2.updateUKSavingsAccountName.fixture.UpdateUKSavingsAccountNameFixtures.{requestBodyModel, validRequestJson}
import v2.updateUKSavingsAccountName.model.request.UpdateUKSavingsAccountNameRequest

class UpdateUKSavingsAccountNameValidatorSpec extends UnitSpec with JsonErrorValidators {

  private implicit val correlationId: String = "1234"

  private val validNino: String         = "AA123456A"
  val validSavingsAccountId: String            = "ABCDE0123456789"

  private val parsedNino: Nino                 = Nino(validNino)

  private def validator(nino: String,
                        savingsAccountId: String,
                        body: JsValue): UpdateUKSavingsAccountNameValidator =
    new UpdateUKSavingsAccountNameValidator(nino, savingsAccountId, body)

  "running a validation" should {
    "return no errors" when {
      "a full valid request is supplied" in {
        val result: Either[ErrorWrapper, UpdateUKSavingsAccountNameRequest] =
          validator(validNino, validSavingsAccountId, validRequestJson).validateAndWrapResult()

        result shouldBe Right(UpdateUKSavingsAccountNameRequest(parsedNino, savingsAccountId, requestBodyModel))
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        val result: Either[ErrorWrapper, UpdateUKSavingsAccountNameRequest] =
          validator("A12344A", validSavingsAccountId, validRequestJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }
    }
    "return SavingsAccountIdFormatError error" when {
      "an invalid savings account id is supplied" in {
        val result: Either[ErrorWrapper, UpdateUKSavingsAccountNameRequest] =
          validator(validNino, "g434", validRequestJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, SavingsAccountIdFormatError))
      }
    }


      "passed a body with a missing mandatory field" in {
        val result: Either[ErrorWrapper, UpdateUKSavingsAccountNameRequest] =
          validator(validNino, validSavingsAccountId, validRequestJson.removeProperty("/accountName")).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath("/accountName")))
      }

      validRequestJson.as[JsObject].fields.foreach { case (field, _) =>
        s"passed a body with an incorrect type for field $field" in {
          val invalidJson: JsValue = validRequestJson.update(s"/$field", JsObject.empty)

          val result: Either[ErrorWrapper, UpdateUKSavingsAccountNameRequest] =
            validator(validNino, validSavingsAccountId, invalidJson).validateAndWrapResult()

          result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath(s"/$field")))
        }
      }
    }


    "return multiple errors" when {
      "request supplied has multiple errors" in {
        val result: Either[ErrorWrapper, UpdateUKSavingsAccountNameRequest] =
          validator("A12344A", "GWD", validRequestJson).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(List(NinoFormatError, TaxYearFormatError))))
      }
    }


}
