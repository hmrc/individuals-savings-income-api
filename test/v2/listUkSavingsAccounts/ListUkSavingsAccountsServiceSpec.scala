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

package v2.listUkSavingsAccounts

import models.domain.SavingsAccountId
import models.errors.SavingsAccountIdFormatError
import shared.controllers.EndpointLogContext
import shared.models.domain.Nino
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v2.listUkSavingsAccounts.def1.model.request.Def1_ListUkSavingsAccountsRequestData
import v2.listUkSavingsAccounts.def1.model.response.{Def1_ListUkSavingsAccountsResponse, Def1_UkSavingsAccount}

import scala.concurrent.Future

class ListUkSavingsAccountsServiceSpec extends ServiceSpec {

  private val nino             = "AA112233A"
  private val savingsAccountId = SavingsAccountId("SAVKB2UVwUTBQGJ")

  private val requestData = Def1_ListUkSavingsAccountsRequestData(Nino(nino), Some(savingsAccountId))

  private val validResponse = Def1_ListUkSavingsAccountsResponse(
    savingsAccounts = Some(
      Seq(
        Def1_UkSavingsAccount(savingsAccountId = "000000000000001", accountName = Some("Bank Account 1")),
        Def1_UkSavingsAccount(savingsAccountId = "000000000000002", accountName = Some("Bank Account 2")),
        Def1_UkSavingsAccount(savingsAccountId = "000000000000003", accountName = Some("Bank Account 3"))
      )
    )
  )

  trait Test extends MockListUkSavingsAccountsConnector {
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    val service: ListUkSavingsAccountsService = new ListUkSavingsAccountsService(connector = mockListUkSavingsAccountsConnector)
  }

  "ListUkSavingsAccountsService" when {
    "listUkSavingsAccounts" must {
      "return correct result for a success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, validResponse))

        MockListUkSavingsAccountsConnector
          .listUkSavingsAccounts(requestData)
          .returns(Future.successful(outcome))

        await(service.listUkSavingsAccounts(requestData)) shouldBe outcome
      }

      "map errors according to spec" when {

        def serviceError(ifsErrorCode: String, error: MtdError): Unit =
          s"a $ifsErrorCode error is returned from the service" in new Test {

            MockListUkSavingsAccountsConnector
              .listUkSavingsAccounts(requestData)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(ifsErrorCode))))))

            await(service.listUkSavingsAccounts(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val input = List(
          ("INVALID_ID_TYPE", InternalError),
          ("INVALID_IDVALUE", NinoFormatError),
          ("INVALID_INCOMESOURCETYPE", InternalError),
          ("INVALID_TAXYEAR", InternalError),
          ("INVALID_INCOMESOURCEID", SavingsAccountIdFormatError),
          ("INVALID_ENDDATE", InternalError),
          ("NOT_FOUND", NotFoundError),
          ("SERVER_ERROR", InternalError),
          ("SERVICE_UNAVAILABLE", InternalError)
        )

        input.foreach(args => (serviceError _).tupled(args))
      }
    }
  }

}
