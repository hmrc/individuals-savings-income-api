/*
 * Copyright 2022 HM Revenue & Customs
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

package v1r7.services

import api.controllers.EndpointLogContext
import api.models.domain.Nino
import api.models.errors.{DownstreamErrorCode, DownstreamErrors, ErrorWrapper, MtdError, NinoFormatError, StandardDownstreamError, TaxYearFormatError}
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import v1r7.fixtures.residentialPropertyDisposals.CreateAmendCgtResidentialPropertyDisposalsServiceConnectorFixture.requestBodyModel
import v1r7.mocks.connectors.MockCreateAmendCgtResidentialPropertyDisposalsConnector
import v1r7.models.request.createAmendCgtResidentialPropertyDisposals.CreateAmendCgtResidentialPropertyDisposalsRequest

import scala.concurrent.Future

class CreateAmendCgtResidentialPropertyDisposalsServiceSpec extends ServiceSpec {

  private val nino    = "AA112233A"
  private val taxYear = "2019-20"

  val createAmendCgtResidentialPropertyDisposalsRequest: CreateAmendCgtResidentialPropertyDisposalsRequest =
    CreateAmendCgtResidentialPropertyDisposalsRequest(
      nino = Nino(nino),
      taxYear = taxYear,
      body = requestBodyModel
    )

  trait Test extends MockCreateAmendCgtResidentialPropertyDisposalsConnector {
    implicit val logContext: EndpointLogContext = EndpointLogContext("Other", "amend")

    val service: CreateAmendCgtResidentialPropertyDisposalsService = new CreateAmendCgtResidentialPropertyDisposalsService(
      connector = mockCreateAmendCgtResidentialPropertyDisposalsConnector
    )

  }

  "CreateAmendCgtResidentialPropertyDisposalsService" when {
    "createAndAmend" must {
      "return correct result for a success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockCreateAmendCgtResidentialPropertyDisposalsConnector
          .createAndAmend(createAmendCgtResidentialPropertyDisposalsRequest)
          .returns(Future.successful(outcome))

        await(service.createAndAmend(createAmendCgtResidentialPropertyDisposalsRequest)) shouldBe outcome
      }

      "map errors according to spec" when {

        def serviceError(desErrorCode: String, error: MtdError): Unit =
          s"a $desErrorCode error is returned from the connector" in new Test {

            MockCreateAmendCgtResidentialPropertyDisposalsConnector
              .createAndAmend(createAmendCgtResidentialPropertyDisposalsRequest)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(desErrorCode))))))

            await(service.createAndAmend(createAmendCgtResidentialPropertyDisposalsRequest)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        def failuresArrayError(desErrorCode: String, error: MtdError): Unit =
          s"a $desErrorCode error is returned from the connector in a failures array" in new Test {

            MockCreateAmendCgtResidentialPropertyDisposalsConnector
              .createAndAmend(createAmendCgtResidentialPropertyDisposalsRequest)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors(List(DownstreamErrorCode(desErrorCode)))))))

            await(service.createAndAmend(createAmendCgtResidentialPropertyDisposalsRequest)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val input = Seq(
          ("INVALID_TAXABLE_ENTITY_ID", NinoFormatError),
          ("INVALID_TAX_YEAR", TaxYearFormatError),
          ("INVALID_CORRELATIONID", StandardDownstreamError),
          ("INVALID_PAYLOAD", StandardDownstreamError),
          ("INVALID_DISPOSAL_DATE", StandardDownstreamError),
          ("INVALID_COMPLETION_DATE", StandardDownstreamError),
          ("INVALID_ACQUISITION_DATE", StandardDownstreamError),
          ("SERVER_ERROR", StandardDownstreamError),
          ("SERVICE_UNAVAILABLE", StandardDownstreamError)
        )

        input.foreach(args => (serviceError _).tupled(args))
        input.foreach(args => (failuresArrayError _).tupled(args))
      }
    }
  }

}
