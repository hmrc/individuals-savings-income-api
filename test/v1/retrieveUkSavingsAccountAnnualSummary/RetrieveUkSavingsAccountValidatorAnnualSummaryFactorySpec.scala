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

package v1.retrieveUkSavingsAccountAnnualSummary

import shared.UnitSpec
import shared.config.MockAppConfig
import shared.controllers.validators.Validator
import config.MockSavingsConfig
import v1.retrieveUkSavingsAccountAnnualSummary.def1.Def1_RetrieveUkSavingsAccountAnnualSummaryValidator
import v1.retrieveUkSavingsAccountAnnualSummary.model.request.RetrieveUkSavingsAccountAnnualSummaryRequestData

class RetrieveUkSavingsAccountValidatorAnnualSummaryFactorySpec extends UnitSpec with MockAppConfig with MockSavingsConfig {
  private val validNino             = "AA123456A"
  private val validTaxYear          = "2021-22"
  private val validSavingsAccountId = "SAVKB2UVwUTBQGJ"

  val validator = new RetrieveUkSavingsAccountAnnualSummaryValidatorFactory(mockAppConfig, mockSavingsConfig)

  "validator()" when {

    "given any request with a valid tax year" should {
      "return the Validator for schema definition 1" in {

        val result: Validator[RetrieveUkSavingsAccountAnnualSummaryRequestData] =
          validator.validator(validNino, validTaxYear, validSavingsAccountId)

        result shouldBe a[Def1_RetrieveUkSavingsAccountAnnualSummaryValidator]
      }
    }

  }

}