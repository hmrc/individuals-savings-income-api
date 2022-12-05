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

package v1.services

import api.connectors.DownstreamUri.DesUri
import api.controllers.EndpointLogContext
import api.models.domain.{MtdSourceEnum, Nino, TaxYear}
import v1.mocks.connectors.MockRetrieveAllResidentialPropertyCgtConnector
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.ServiceSpec
import play.api.libs.json.{Format, Json}
import v1.models.request.retrieveAllResidentialPropertyCgt.RetrieveAllResidentialPropertyCgtRequest
import v1.models.response.retrieveAllResidentialPropertyCgt.{RetrieveAllResidentialPropertyCgtResponse}

import scala.concurrent.Future

class RetrieveAllResidentialPropertyCgtServiceSpec extends ServiceSpec {

  private val nino    = "AA112233A"
  private val taxYear = "2019-20"

  val request: RetrieveAllResidentialPropertyCgtRequest = RetrieveAllResidentialPropertyCgtRequest(
    nino = Nino(nino),
    taxYear = TaxYear.fromMtd(taxYear),
    source = MtdSourceEnum.latest
  )

  val response: RetrieveAllResidentialPropertyCgtResponse = RetrieveAllResidentialPropertyCgtResponse(
    ppdService = None,
    customerAddedDisposals = None
  )

  trait Test extends MockRetrieveAllResidentialPropertyCgtConnector {

    case class Data(field: Option[String])

    object Data {
      implicit val reads: Format[Data] = Json.format[Data]
    }

    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")
    implicit val deleteDesUri: DesUri[Unit]     = DesUri[Unit](s"income-tax/income/savings/$nino/$taxYear")
    implicit val retrieveDesUri: DesUri[Data]   = DesUri[Data](s"income-tax/income/savings/$nino/$taxYear")

    val service: RetrieveAllResidentialPropertyCgtService = new RetrieveAllResidentialPropertyCgtService(
      connector = mockRetrieveAllResidentialPropertyCgtConnector
    )

  }

  "RetrieveAllResidentialPropertyCgtService" when {

    "retrieve" must {
      "return correct result for a success" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, response))

        MockRetrieveAllResidentialPropertyCgtConnector
          .retrieve(request)
          .returns(Future.successful(outcome))

        await(service.retrieve(request)) shouldBe outcome
      }

//      "return a NotFoundError for an empty response" in new Test {
//        val outcome = Right(ResponseWrapper(correlationId, Data(None)))
//
//        MockRetrieveAllResidentialPropertyCgtConnector
//          .retrieve(request)
//          .returns(Future.successful(outcome))
//
//        await(service.retrieve(request)) shouldBe Left(ErrorWrapper(correlationId, NotFoundError))
//      }

      "map errors according to spec" when {

        def serviceError(desErrorCode: String, error: MtdError): Unit =
          s"a $desErrorCode error is returned from the service" in new Test {

            MockRetrieveAllResidentialPropertyCgtConnector
              .retrieve(request)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(desErrorCode))))))

            await(service.retrieve(request)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val input = Seq(
          ("INVALID_TAXABLE_ENTITY_ID", NinoFormatError),
          ("INVALID_TAX_YEAR", TaxYearFormatError),
          ("INVALID_CORRELATIONID", StandardDownstreamError),
          ("NO_DATA_FOUND", NotFoundError),
          ("SERVER_ERROR", StandardDownstreamError),
          ("SERVICE_UNAVAILABLE", StandardDownstreamError)
        )

        input.foreach(args => (serviceError _).tupled(args))
      }
    }
  }

}
