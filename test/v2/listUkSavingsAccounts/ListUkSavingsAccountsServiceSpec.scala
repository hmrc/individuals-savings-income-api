/*
 * Copyright 2026 HM Revenue & Customs
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

package v2.listUkSavingsAccounts

import api.connectors.DownstreamOutcome
import api.models.domain.Nino
import api.models.errors.*
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import models.domain.SavingsAccountId
import v2.listUkSavingsAccounts.def1.model.request.Def1_ListUkSavingsAccountsRequestData
import v2.listUkSavingsAccounts.def1.model.response.{Def1_ListUkSavingsAccountsResponse, Def1_UkSavingsAccount}
import v2.listUkSavingsAccounts.model.request.ListUkSavingsAccountsRequestData
import v2.listUkSavingsAccounts.model.response.ListUkSavingsAccountsResponse

import scala.concurrent.Future

class ListUkSavingsAccountsServiceSpec extends ServiceSpec {

  private val requestData: ListUkSavingsAccountsRequestData = Def1_ListUkSavingsAccountsRequestData(
    nino = Nino("AA112233A"),
    savingsAccountId = Some(SavingsAccountId("SAVKB2UVwUTBQGJ"))
  )

  private val validResponse: ListUkSavingsAccountsResponse = Def1_ListUkSavingsAccountsResponse(
    savingsAccounts = Some(
      Seq(
        Def1_UkSavingsAccount(savingsAccountId = "000000000000001", accountName = Some("Bank Account 1")),
        Def1_UkSavingsAccount(savingsAccountId = "000000000000002", accountName = Some("Bank Account 2")),
        Def1_UkSavingsAccount(savingsAccountId = "000000000000003", accountName = Some("Bank Account 3"))
      )
    )
  )

  private trait Test extends MockListUkSavingsAccountsConnector {
    val service: ListUkSavingsAccountsService = new ListUkSavingsAccountsService(connector = mockListUkSavingsAccountsConnector)
  }

  "ListUkSavingsAccountsService" when {
    "listUkSavingsAccounts" must {
      "return correct result for a success" in new Test {
        val outcome: DownstreamOutcome[ListUkSavingsAccountsResponse] = Right(ResponseWrapper(correlationId, validResponse))

        MockListUkSavingsAccountsConnector
          .listUkSavingsAccounts(requestData)
          .returns(Future.successful(outcome))

        await(service.listUkSavingsAccounts(requestData)) shouldBe outcome
      }

      "deduplicate savings accounts by savingsAccountId" in new Test {
        val responseWithDuplicates: ListUkSavingsAccountsResponse = Def1_ListUkSavingsAccountsResponse(
          savingsAccounts = Some(
            Seq(
              Def1_UkSavingsAccount(savingsAccountId = "000000000000001", accountName = Some("Bank Account 1")),
              Def1_UkSavingsAccount(savingsAccountId = "000000000000001", accountName = Some("Duplicate Bank Account 1")),
              Def1_UkSavingsAccount(savingsAccountId = "000000000000002", accountName = Some("Bank Account 2")),
              Def1_UkSavingsAccount(savingsAccountId = "000000000000003", accountName = Some("Bank Account 3"))
            )
          )
        )

        MockListUkSavingsAccountsConnector
          .listUkSavingsAccounts(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseWithDuplicates))))

        await(service.listUkSavingsAccounts(requestData)) shouldBe Right(ResponseWrapper(correlationId, validResponse))
      }

      "preserve missing savings accounts" in new Test {
        val outcome: DownstreamOutcome[ListUkSavingsAccountsResponse] =
          Right(ResponseWrapper(correlationId, Def1_ListUkSavingsAccountsResponse(savingsAccounts = None)))

        MockListUkSavingsAccountsConnector
          .listUkSavingsAccounts(requestData)
          .returns(Future.successful(outcome))

        await(service.listUkSavingsAccounts(requestData)) shouldBe outcome
      }

      "map errors according to spec" when {
        def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
          s"a $downstreamErrorCode error is returned from the service" in new Test {

            MockListUkSavingsAccountsConnector
              .listUkSavingsAccounts(requestData)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

            await(service.listUkSavingsAccounts(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val errors: Seq[(String, MtdError)] = List(
          ("1215", NinoFormatError),
          ("1117", InternalError),
          ("1122", InternalError),
          ("1007", SavingsAccountIdFormatError),
          ("1229", InternalError),
          ("5010", NotFoundError)
        )

        errors.foreach(serviceError.tupled)
      }
    }
  }

}
