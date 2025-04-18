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

package v2.addUkSavingsAccount.def1.model.request

import play.api.Configuration
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import shared.config.MockSharedAppConfig
import shared.utils.UnitSpec

class Def1_AddUkSavingsAccountRequestBodySpec extends UnitSpec with MockSharedAppConfig {

  val mtdJson: JsValue = Json.parse(
    """
      |{
      |   "accountName": "Shares savings account"
      |}
    """.stripMargin
  )

  def downstreamJson(incomeSourceType: String): JsValue = Json.parse(
    s"""
      |{
      |    "incomeSourceType": "$incomeSourceType",
      |    "incomeSourceName": "Shares savings account"
      |}
    """.stripMargin
  )

  val model: Def1_AddUkSavingsAccountRequestBody = Def1_AddUkSavingsAccountRequestBody("Shares savings account")

  "Def1_AddUkSavingsAccountRequestBody" when {
    "read from a valid JSON" should {
      "produce the expected object" in {
        mtdJson.as[Def1_AddUkSavingsAccountRequestBody] shouldBe model
      }
    }

    "read from empty JSON" should {
      "produce a JsError" in {
        val invalidJson = JsObject.empty
        invalidJson.validate[Def1_AddUkSavingsAccountRequestBody] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JSON when feature switch is disabled" in {
        MockedSharedAppConfig.featureSwitchConfig returns Configuration("des_hip_migration_1393.enabled" -> false)
        Json.toJson(model) shouldBe downstreamJson("interest-from-uk-banks")
      }

      "produce the expected JSON when feature switch is enabled" in {
        MockedSharedAppConfig.featureSwitchConfig returns Configuration("des_hip_migration_1393.enabled" -> true)
        Json.toJson(model) shouldBe downstreamJson("09")
      }
    }
  }

}
