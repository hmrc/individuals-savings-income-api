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

package v2.addUkSavingsAccount

import play.api.libs.json.Json
import shared.controllers.validators.Validator
import shared.utils.UnitSpec
import v2.addUkSavingsAccount.def1.Def1_AddUkSavingsAccountValidator
import v2.addUkSavingsAccount.model.request.AddUkSavingsAccountRequestData

class AddUkSavingsAccountValidatorFactorySpec extends UnitSpec {
  private val validNino = "AA123456A"

  private val validRequestBodyJson = Json.parse(
    """
      |{
      |  "accountName": "Shares savings account"
      |}
    """.stripMargin
  )

  private val validatorFactory = new AddUkSavingsAccountValidatorFactory()

  "validator()" when {
    "given any tax year" should {
      "return the Validator for schema definition 1" in {
        val requestBody = validRequestBodyJson
        val result: Validator[AddUkSavingsAccountRequestData] =
          validatorFactory.validator(validNino, requestBody)
        result shouldBe a[Def1_AddUkSavingsAccountValidator]
      }
    }
  }

}
