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

package v2.updateUKSavingsAccountName

import shared.models.domain.Nino
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.{ServiceOutcome, ServiceSpec}
import v2.updateUKSavingsAccountName.fixture.UpdateUKSavingsAccountNameFixtures.requestBodyModel
import v2.updateUKSavingsAccountName.model.request.UpdateUKSavingsAccountNameRequest
import models.errors.SavingsAccountIdFormatError

import scala.concurrent.Future

class UpdateUKSavingsAccountNameServiceSpec extends ServiceSpec {

  private val nino: Nino                 = Nino("AA123456A")
  val savingsAccountId: String            = "ABCDE0123456789"

  "UpdateUKSavingsAccountNameService" when {
    "update" should {
      "return correct result for a success" in new Test {
        val outcome: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        MockUpdateUKSavingsAccountNameConnector
          .createAmend(request)
          .returns(Future.successful(outcome))

        val result: ServiceOutcome[Unit] = await(service.update(request))

        result shouldBe outcome
      }

      "map errors according to spec" when {
        def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
          s"a $downstreamErrorCode error is returned from the service" in new Test {

            MockUpdateUKSavingsAccountNameConnector
              .createAmend(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

            val result: ServiceOutcome[Unit] = await(service.update(request))

            result shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val errors = List(
          ("1215", NinoFormatError),
          ("1007", SavingsAccountIdFormatError),
          ("1000", InternalError),
          ("5010", NotFoundError)

        )

        errors.foreach(args => (serviceError _).tupled(args))
      }
    }
  }

  private trait Test extends MockUpdateUKSavingsAccountNameConnector {

    val request: UpdateUKSavingsAccountNameRequest = UpdateUKSavingsAccountNameRequest(
      nino = nino,
      SavingsAccountId(incomeSourceId),
      body = requestBodyModel
    )

    val service: UpdateUKSavingsAccountNameService = new UpdateUKSavingsAccountNameService(
      connector = mockUpdateUKSavingsAccountNameConnector
    )

  }

}
