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
import v1.fixtures.insurancePolicies.AmendInsurancePoliciesFixture._
import v1.mocks.MockHttpClient
import v1.models.domain.DesTaxYear
import v1.models.outcomes.ResponseWrapper
import v1.models.request.insurancePolicies.amend.{AmendRequest, AmendRequestBody}

import scala.concurrent.Future


class AmendInsurancePoliciesConnectorSpec extends ConnectorSpec {

  val nino: String = "AA111111A"
  val taxYear: String = "2019"

  val amendInsurancePolsRequestBody: AmendRequestBody = AmendRequestBody(
      lifeInsurance = Some(Seq(fullLifeInsuranceModel)),
      capitalRedemption = Some(Seq(fullCapitalRedemptionModel)),
      lifeAnnuity = Some(Seq(fullLifeAnnuityModel)),
      voidedIsa = Some(Seq(fullVoidedIsaModel)),
      foreign = Some(Seq(fullForeignModel))
  )

  val amendInsurancePolsRequest: AmendRequest = AmendRequest(
      nino = Nino(nino),
      taxYear = DesTaxYear(taxYear),
      body = amendInsurancePolsRequestBody
  )

  class Test extends MockHttpClient with MockAppConfig {

    val connector: AmendInsurancePoliciesConnector = new AmendInsurancePoliciesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
  }

  "AmendInsurancePoliciesConnector" when {
    "amendInsurancePolicies" must {
      "return a 204 status for a success scenario" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockedHttpClient
          .put(
            url = s"$baseUrl/some-placeholder/insurance-policies/$nino/$taxYear",
            body = amendInsurancePolsRequestBody,
            requiredHeaders = "Environment" -> "des-environment", "Authorization" -> s"Bearer des-token"
          ).returns(Future.successful(outcome))

        await(connector.amendInsurancePolicies(amendInsurancePolsRequest)) shouldBe outcome
      }
    }
  }
}