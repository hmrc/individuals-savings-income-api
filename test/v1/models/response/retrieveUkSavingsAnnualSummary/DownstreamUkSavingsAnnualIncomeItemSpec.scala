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

package v1.models.response.retrieveUkSavingsAnnualSummary

import shared.UnitSpec

class DownstreamUkSavingsAnnualIncomeItemSpec extends UnitSpec {

  "DownstreamUkSavingsAnnualIncomeItem" must {
    "be convertible to MTD" in {
      DownstreamUkSavingsAnnualIncomeItem("ignored", taxedUkInterest = Some(1.12), untaxedUkInterest = Some(2.12)).toMtd shouldBe
        RetrieveUkSavingsAnnualSummaryResponse(taxedUkInterest = Some(1.12), untaxedUkInterest = Some(2.12))
    }
  }

}
