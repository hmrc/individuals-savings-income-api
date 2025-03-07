/*
 * Copyright 2024 HM Revenue & Customs
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

package config

import play.api.Configuration
import shared.utils.UnitSpec

class SavingsFeatureSwitchesSpec extends UnitSpec {

  def aFeatureSwitchFor(featureName: String, switch: SavingsFeatureSwitches => Boolean): Unit =
    s"a feature switch for $featureName" should {
      "be true" when {
        "absent from the config" in {
          val configuration   = Configuration.empty
          val featureSwitches = SavingsFeatureSwitches(configuration)
          switch(featureSwitches) shouldBe true
        }

        "enabled" in {
          val configuration   = Configuration(featureName + ".enabled" -> true)
          val featureSwitches = SavingsFeatureSwitches(configuration)
          switch(featureSwitches) shouldBe true
        }
      }

      "be false" when {
        "disabled" in {
          val configuration   = Configuration(featureName + ".enabled" -> false)
          val featureSwitches = SavingsFeatureSwitches(configuration)
          switch(featureSwitches) shouldBe false
        }
      }
    }

  "isDesIf_MigrationEnabled" should {
    behave like aFeatureSwitchFor("desIf_Migration", _.isDesIf_MigrationEnabled)
  }

}
