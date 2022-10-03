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

package v1.requestParsers.validators

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

import api.mocks.MockCurrentDateTime
import api.models.errors._
import api.requestParsers.validators.DeleteOtherEmploymentIncomeRequestValidator
import config.AppConfig
import mocks.MockAppConfig
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import support.UnitSpec
import utils.CurrentDateTime
import v1.models.request.deleteOtherEmploymentIncome.DeleteOtherEmploymentIncomeRequestRawData

class DeleteOtherEmploymentIncomeValidatorSpec extends UnitSpec with MockAppConfig {

  object Data {
    val validNino    = "AA123456A"
    val validTaxYear = "2019-20"
  }

  import Data._

  class Test extends MockAppConfig with MockCurrentDateTime {

    implicit val appConfig: AppConfig              = mockAppConfig
    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter       = DateTimeFormat.forPattern("yyyy-MM-dd")

    val validator = new DeleteOtherEmploymentIncomeRequestValidator()

    MockCurrentDateTime.getDateTime
      .returns(DateTime.parse("2021-07-29", dateTimeFormatter))
      .anyNumberOfTimes()

    private val MINIMUM_YEAR = 2018
    MockedAppConfig.minimumPermittedTaxYear returns MINIMUM_YEAR
  }

  "running validation" should {
    "return no errors" when {
      "passed a valid raw request model" in new Test {
        validator.validate(DeleteOtherEmploymentIncomeRequestRawData(validNino, validTaxYear)) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "passed an invalid nino" in new Test {
        validator.validate(DeleteOtherEmploymentIncomeRequestRawData("A12344A", validTaxYear)) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "passed an invalid taxYear" in new Test {
        validator.validate(DeleteOtherEmploymentIncomeRequestRawData(validNino, "201920")) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "an invalid tax year is supplied" in new Test {
        validator.validate(DeleteOtherEmploymentIncomeRequestRawData(validNino, "2016-17")) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return RuleTaxYearRangeInvalidError error" when {
      "an invalid tax year is supplied" in new Test {
        validator.validate(DeleteOtherEmploymentIncomeRequestRawData(validNino, "2019-23")) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }
    }

  }

}