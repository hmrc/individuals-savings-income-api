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

package v1.controllers.validators.mocks

import shared.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import v1.controllers.validators.CreateAmendSavingsValidatorFactory
import v1.models.request.amendSavings.CreateAmendSavingsRequestData

trait MockCreateAmendSavingsValidatorFactory extends MockFactory {

  val mockCreateAmendSavingsValidatorFactory: CreateAmendSavingsValidatorFactory =
    mock[CreateAmendSavingsValidatorFactory]

  object MockedCreateAmendSavingsValidatorFactory {

    def validator(): CallHandler[Validator[CreateAmendSavingsRequestData]] =
      (mockCreateAmendSavingsValidatorFactory.validator(_: String, _: String, _: JsValue)).expects(*, *, *)

  }

  def willUseValidator(use: Validator[CreateAmendSavingsRequestData]): CallHandler[Validator[CreateAmendSavingsRequestData]] = {
    MockedCreateAmendSavingsValidatorFactory
      .validator()
      .anyNumberOfTimes()
      .returns(use)
  }

  def returningSuccess(result: CreateAmendSavingsRequestData): Validator[CreateAmendSavingsRequestData] =
    new Validator[CreateAmendSavingsRequestData] {
      def validate: Validated[Seq[MtdError], CreateAmendSavingsRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[CreateAmendSavingsRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[CreateAmendSavingsRequestData] =
    new Validator[CreateAmendSavingsRequestData] {
      def validate: Validated[Seq[MtdError], CreateAmendSavingsRequestData] = Invalid(result)
    }

}
