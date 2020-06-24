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

package v1.connectors

import mocks.MockAppConfig
import uk.gov.hmrc.domain.Nino
import v1.mocks.MockHttpClient
import v1.models.domain.DesTaxYear
import v1.models.outcomes.ResponseWrapper
import v1.models.request.amendForeign.{AmendForeignRequest, AmendForeignRequestBody, ForeignEarnings, UnremittableForeignIncomeItem}

import scala.concurrent.Future

class AmendForeignConnectorSpec extends ConnectorSpec {

  private val nino: String = "AA111111A"
  private val taxYear: String = "2019"

  private val foreignEarningsModel = ForeignEarnings(
    customerReference = Some("ref"),
    earningsNotTaxableUK = Some(111.11)
  )

  private val unremittableForeignIncomeModel = Seq(
    UnremittableForeignIncomeItem(
      countryCode = "DEU",
      amountInForeignCurrency = Some(222.22),
      amountTaxPaid = Some(333.33)
    ),
    UnremittableForeignIncomeItem(
      countryCode = "FRA",
      amountInForeignCurrency = Some(444.44),
      amountTaxPaid = Some(555.55)
    )
  )

  private val amendForeignRequestBody = AmendForeignRequestBody(
    foreignEarnings = Some(foreignEarningsModel),
    unremittableForeignIncome = Some(unremittableForeignIncomeModel)
  )

  val amendForeignRequest: AmendForeignRequest = AmendForeignRequest(
    nino = Nino(nino),
    taxYear = DesTaxYear(taxYear),
    body = amendForeignRequestBody
  )

  class Test extends MockHttpClient with MockAppConfig {

    val connector: AmendForeignConnector = new AmendForeignConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
  }

  "AmendForeignConnector" when {
    "amendForeign" must {
      "return a 204 status for a success scenario" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockedHttpClient
          .put(
            url = s"$baseUrl/some-placeholder/foreign/$nino/$taxYear",
            body = amendForeignRequestBody,
            requiredHeaders = "Environment" -> "des-environment", "Authorization" -> s"Bearer des-token"
          ).returns(Future.successful(outcome))

        await(connector.amendForeign(amendForeignRequest)) shouldBe outcome
      }
    }
  }
}