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

package v1.connectors


import mocks.MockFeatureSwitches
import models.domain.SavingsAccountId
import shared.connectors.ConnectorSpec
import shared.models.domain.{Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import v1.models.request.retrieveUkSavingsAnnualSummary.RetrieveUkSavingsAnnualSummaryRequestData
import v1.models.response.retrieveUkSavingsAnnualSummary.{DownstreamUkSavingsAnnualIncomeItem, DownstreamUkSavingsAnnualIncomeResponse}

import scala.concurrent.Future

class RetrieveUkSavingsAccountAnnualSummaryConnectorSpec extends ConnectorSpec with MockFeatureSwitches {

  val nino: String           = "AA111111A"
  val incomeSourceId: String = "SAVKB2UVwUTBQGJ"

  trait Test {
    _: ConnectorTest =>

    def taxYear: TaxYear

    val request: RetrieveUkSavingsAnnualSummaryRequestData =
      RetrieveUkSavingsAnnualSummaryRequestData(
        Nino(nino),
        taxYear,
        SavingsAccountId(incomeSourceId)
      )

    val response: DownstreamUkSavingsAnnualIncomeResponse = DownstreamUkSavingsAnnualIncomeResponse(
      Seq(
        DownstreamUkSavingsAnnualIncomeItem(
          incomeSourceId = incomeSourceId,
          taxedUkInterest = Some(1230.55),
          untaxedUkInterest = Some(1230.55)
        ))
    )

    val connector: RetrieveUkSavingsAccountAnnualSummaryConnector = new RetrieveUkSavingsAccountAnnualSummaryConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )(mockFeatureSwitches)

  }

  "RetrieveUkSavingsAccountAnnualSummaryConnector" when {
    "retrieveUkSavingsAccountAnnualSummary called" must {
      "return a 200 status for a success scenario" in new DesTest with Test {

        MockFeatureSwitches.isDesIf_MigrationEnabled.returns(false)
        def taxYear: TaxYear = TaxYear.fromMtd("2019-20")
        private val outcome = Right(ResponseWrapper(correlationId, response))
        willGet(
          s"$baseUrl/income-tax/nino/$nino/income-source/savings/annual/2020?incomeSourceId=$incomeSourceId"
        ) returns Future.successful(outcome)

        await(connector.retrieveUkSavingsAccountAnnualSummary(request)) shouldBe outcome
      }

      "return a 200 status for a success scenario when desIf_Migration is enabled" in new IfsTest with Test {

        MockFeatureSwitches.isDesIf_MigrationEnabled.returns(true)
        def taxYear: TaxYear = TaxYear.fromMtd("2019-20")
        private val outcome = Right(ResponseWrapper(correlationId, response))
        willGet(
          s"$baseUrl/income-tax/nino/$nino/income-source/savings/annual/2020?incomeSourceId=$incomeSourceId"
        ) returns Future.successful(outcome)

        await(connector.retrieveUkSavingsAccountAnnualSummary(request)) shouldBe outcome
      }
    }

    "retrieveUkSavingsAccountAnnualSummary called for a TYS tax year" must {
      "return a 200 status for a success scenario" in new TysIfsTest with Test {
        def taxYear: TaxYear = TaxYear.fromMtd("2023-24")

        private val outcome = Right(ResponseWrapper(correlationId, response))

        willGet(
          s"$baseUrl/income-tax/23-24/$nino/income-source/savings/annual?incomeSourceId=$incomeSourceId"
        ) returns Future.successful(outcome)

        await(connector.retrieveUkSavingsAccountAnnualSummary(request)) shouldBe outcome
      }
    }
  }

}
