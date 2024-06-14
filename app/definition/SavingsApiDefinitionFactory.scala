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

package definition

import play.api.Logger
import shared.config.AppConfig
import shared.definition.{APIDefinition, APIStatus, APIVersion, ApiDefinitionFactory, Definition, Scope}
import shared.routing.{Version, Version1}
import uk.gov.hmrc.auth.core.ConfidenceLevel

import javax.inject.{Inject, Singleton}

@Singleton
class SavingsApiDefinitionFactory @Inject()(protected val appConfig: AppConfig) extends ApiDefinitionFactory{

  override val readScope      = "read:self-assessment"
  override val writeScope     = "write:self-assessment"
  override lazy val logger: Logger = Logger(this.getClass)

  override lazy val confidenceLevel: ConfidenceLevel = {
    val clConfig = appConfig.confidenceLevelConfig

    if (clConfig.definitionEnabled) clConfig.confidenceLevel else ConfidenceLevel.L50
  }

  lazy val definition: Definition =
    Definition(
      scopes = Seq(
        Scope(
          key = readScope,
          name = "View your Self Assessment information",
          description = "Allow read access to self assessment data",
          confidenceLevel = confidenceLevel
        ),
        Scope(
          key = writeScope,
          name = "Change your Self Assessment information",
          description = "Allow write access to self assessment data",
          confidenceLevel = confidenceLevel
        )
      ),
      api = APIDefinition(
        name = "Individuals Savings Income (MTD)",
        description = "An API for providing individuals savings income data",
        context = appConfig.apiGatewayContext,
        categories = Seq("INCOME_TAX_MTD"),
        versions = Seq(
          APIVersion(
            version = Version1,
            status = buildAPIStatus(Version1),
            endpointsEnabled = appConfig.endpointsEnabled(Version1)
          )
        ),
        requiresTrust = None
      )
    )

  override def buildAPIStatus(version: Version): APIStatus = {
    APIStatus.parser
      .lift(appConfig.apiStatus(version))
      .getOrElse {
        logger.error(s"[ApiDefinition][buildApiStatus] no API Status found in config.  Reverting to Alpha")
        APIStatus.ALPHA
      }
  }

}