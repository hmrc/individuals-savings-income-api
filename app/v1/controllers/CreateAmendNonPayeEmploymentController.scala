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

package v1.controllers

import api.controllers.{ AuthorisedController, BaseController, EndpointLogContext }
import api.models.audit.{ AuditEvent, AuditResponse, GenericAuditDetail }
import api.models.errors._
import api.services.{ AuditService, EnrolmentsAuthService, MtdIdLookupService }
import cats.data.EitherT
import config.AppConfig
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContentAsJson, ControllerComponents }
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.{ IdGenerator, Logging }
import api.hateoas.AmendHateoasBody
import v1.models.request.createAmendNonPayeEmployment.CreateAmendNonPayeEmploymentRawData
import v1.requestParsers.CreateAmendNonPayeEmploymentRequestParser
import v1.services.CreateAmendNonPayeEmploymentService

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class CreateAmendNonPayeEmploymentController @Inject()(val authService: EnrolmentsAuthService,
                                                       val lookupService: MtdIdLookupService,
                                                       appConfig: AppConfig,
                                                       requestParser: CreateAmendNonPayeEmploymentRequestParser,
                                                       service: CreateAmendNonPayeEmploymentService,
                                                       auditService: AuditService,
                                                       cc: ControllerComponents,
                                                       val idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController
    with Logging
    with AmendHateoasBody {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(
      controllerName = "CreateAmendNonPayeEmploymentController",
      endpointName = "createAmendNonPayeEmployment"
    )

  def createAmendNonPayeEmployment(nino: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val correlationId: String = idGenerator.generateCorrelationId
      logger.info(
        s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}]" +
          s"with CorrelationId: $correlationId")

      val rawData: CreateAmendNonPayeEmploymentRawData = CreateAmendNonPayeEmploymentRawData(
        nino = nino,
        taxYear = taxYear,
        body = AnyContentAsJson(request.body)
      )

      val result =
        for {
          parsedRequest   <- EitherT.fromEither[Future](requestParser.parseRequest(rawData))
          serviceResponse <- EitherT(service.createAndAmend(parsedRequest))
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}"
          )

          auditSubmission(
            GenericAuditDetail(
              request.userDetails,
              Map("nino" -> nino, "taxYear" -> taxYear),
              Some(request.body),
              serviceResponse.correlationId,
              AuditResponse(httpStatus = OK, response = Right(Some(amendNonPayeEmploymentHateoasBody(appConfig, nino, taxYear))))
            )
          )

          Ok(amendNonPayeEmploymentHateoasBody(appConfig, nino, taxYear))
            .withApiHeaders(serviceResponse.correlationId)
            .as(MimeTypes.JSON)

        }

      result.leftMap { errorWrapper =>
        val resCorrelationId = errorWrapper.correlationId
        val result           = errorResult(errorWrapper).withApiHeaders(resCorrelationId)
        logger.warn(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
            s"Error response received with CorrelationId: $resCorrelationId")

        auditSubmission(
          GenericAuditDetail(
            request.userDetails,
            Map("nino" -> nino, "taxYear" -> taxYear),
            Some(request.body),
            resCorrelationId,
            AuditResponse(httpStatus = result.header.status, response = Left(errorWrapper.auditErrors))
          )
        )

        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) =
    errorWrapper.error match {
      case BadRequestError | NinoFormatError | TaxYearFormatError | RuleTaxYearNotSupportedError | RuleTaxYearRangeInvalidError |
          RuleTaxYearNotEndedError | CustomMtdError(ValueFormatError.code) | CustomMtdError(RuleIncorrectOrEmptyBodyError.code) =>
        BadRequest(Json.toJson(errorWrapper))
      case NotFoundError           => NotFound(Json.toJson((errorWrapper)))
      case StandardDownstreamError => InternalServerError(Json.toJson(errorWrapper))
      case _                       => unhandledError(errorWrapper)
    }

  private def auditSubmission(details: GenericAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("CreateAmendNonPayeEmploymentIncome", "create-amend-non-paye-employment-income", details)
    auditService.auditEvent(event)
  }

}