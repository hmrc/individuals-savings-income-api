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

package v1.controllers.requestParsers.validators

import v1.controllers.requestParsers.validators.validations._
import v1.models.errors.{MtdError, RuleIncorrectOrEmptyBodyError}
import v1.models.request.amendForeign._

class AmendForeignValidator extends Validator[AmendForeignRawData] with ValueFormatErrorMessages {

  private val validationSet = List(parameterFormatValidation, bodyFormatValidator, bodyValueValidator)

  override def validate(data: AmendForeignRawData): List[MtdError] = {
    run(validationSet, data).distinct
  }

  private def parameterFormatValidation: AmendForeignRawData => List[List[MtdError]] = (data: AmendForeignRawData) => {
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYear)
    )
  }

  private def bodyFormatValidator: AmendForeignRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[AmendForeignRequestBody](data.body.json, RuleIncorrectOrEmptyBodyError)
    )
  }

  private def bodyValueValidator: AmendForeignRawData => List[List[MtdError]] = { data =>

    val requestBodyData = data.body.json.as[AmendForeignRequestBody]

    List(flattenErrors(
      List(
        requestBodyData.foreignEarnings.map { data => validateForeignEarnings(data)}.getOrElse(NoValidationErrors),
        requestBodyData.unremittableForeignIncome.map(_.zipWithIndex.flatMap {
          case (data, index) => validateUnremittableForeignIncome(data, index)
        }).getOrElse(NoValidationErrors).toList
      )
    ))
  }

  private def validateForeignEarnings(foreignEarnings: ForeignEarnings): List[MtdError] = {
    List(
      foreignEarnings.customerReference.fold(NoValidationErrors: List[MtdError]){ ref =>
      CustomerRefValidation.validate(ref).map(
        _.copy(paths = Some(Seq(s"/foreignEarnings/customerReference")))
      )},
      DecimalValueValidation.validateOptional(
        amount = foreignEarnings.earningsNotTaxableUK,
        path = s"/foreignEarnings/earningsNotTaxableUK")
    ).flatten
  }

  private def validateUnremittableForeignIncome(unremittableForeignIncome: UnremittableForeignIncomeItem, arrayIndex: Int): List[MtdError] = {
    List(
      CountryCodeValidation.validate(unremittableForeignIncome.countryCode).map(
        _.copy(paths = Some(Seq(s"/unremittableForeignIncome/$arrayIndex/countryCode")))
      ),
      DecimalValueValidation.validateOptional(
        amount = unremittableForeignIncome.amountInForeignCurrency,
        path = s"/unremittableForeignIncome/$arrayIndex/amountInForeignCurrency"),
      DecimalValueValidation.validateOptional(
        amount = unremittableForeignIncome.amountTaxPaid,
        path = s"/unremittableForeignIncome/$arrayIndex/amountTaxPaid")
    ).flatten
  }

  private def flattenErrors(errors: List[List[MtdError]]): List[MtdError] = {
    errors.flatten.groupBy(_.message).map {case (_, errors) =>

      val baseError = errors.head.copy(paths = Some(Seq.empty[String]))

      errors.fold(baseError)(
        (error1, error2) =>
          error1.copy(paths = Some(error1.paths.getOrElse(Seq.empty[String]) ++ error2.paths.getOrElse(Seq.empty[String])))
      )
    }.toList
  }
}