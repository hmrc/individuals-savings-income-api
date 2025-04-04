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

package config

import play.api.Configuration
import shared.config.{FeatureSwitches, SharedAppConfig}

/** API-specific feature switches.
  */
case class SavingsFeatureSwitches(val featureSwitchConfig: Configuration) extends FeatureSwitches {
  def isDesIf_MigrationEnabled: Boolean = isEnabled("desIf_Migration")
}

object SavingsFeatureSwitches {
  def apply()(implicit appConfig: SharedAppConfig): SavingsFeatureSwitches = SavingsFeatureSwitches(appConfig.featureSwitchConfig)
}
