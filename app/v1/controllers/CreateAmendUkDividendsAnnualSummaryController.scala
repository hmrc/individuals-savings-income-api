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

import api.controllers.{AuthorisedController, BaseController, EndpointLogContext}
import api.hateoas.{AmendHateoasBody, HateoasFactory}
import api.models.audit.{AuditEvent, AuditResponse, FlattenedGenericAuditDetail}
import api.models.errors._
import api.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import cats.data.EitherT
import cats.implicits._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.{IdGenerator, Logging}
import v1.models.request.createAmendUkDividendsIncomeAnnualSummary.CreateAmendUkDividendsIncomeAnnualSummaryRawData
import v1.models.response.createAmendUkDividendsIncomeAnnualSummary.CreateAndAmendUkDividendsIncomeAnnualSummaryHateoasData
import v1.requestParsers.CreateAmendUkDividendsIncomeAnnualSummaryRequestParser
import v1.services.CreateAmendAmendUkDividendsAnnualSummaryService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateAmendUkDividendsAnnualSummaryController @Inject() (val authService: EnrolmentsAuthService,
                                                               val lookupService: MtdIdLookupService,
                                                               requestParser: CreateAmendUkDividendsIncomeAnnualSummaryRequestParser,
                                                               service: CreateAmendAmendUkDividendsAnnualSummaryService,
                                                               auditService: AuditService,
                                                               hateoasFactory: HateoasFactory,
                                                               cc: ControllerComponents,
                                                               val idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController
    with Logging
    with AmendHateoasBody {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(
      controllerName = "CreateAmendUkDividendsAnnualSummaryController",
      endpointName = "createAmendUkDividendsAnnualSummary"
    )

  def createAmendUkDividendsAnnualSummary(nino: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val correlationId: String = idGenerator.generateCorrelationId
      logger.info(
        s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}]" +
          s"with CorrelationId: $correlationId")

      val rawData = CreateAmendUkDividendsIncomeAnnualSummaryRawData(
        nino = nino,
        taxYear = taxYear,
        body = AnyContentAsJson(request.body)
      )

      val result = for {
        parsedRequest   <- EitherT.fromEither[Future](requestParser.parseRequest(rawData))
        serviceResponse <- EitherT(service.createOrAmendAnnualSummary(parsedRequest))
        vendorResponse <- EitherT.fromEither[Future](
          hateoasFactory
            .wrap(serviceResponse.responseData, CreateAndAmendUkDividendsIncomeAnnualSummaryHateoasData(nino, taxYear))
            .asRight[ErrorWrapper])
      } yield {
        logger.info(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Success response received with CorrelationId: ${serviceResponse.correlationId}"
        )

        val jsonResponse = Json.toJson(vendorResponse)

        auditSubmission(
          FlattenedGenericAuditDetail(
            versionNumber = Some("1.0"),
            request.userDetails,
            Map("nino" -> nino, "taxYear" -> taxYear),
            Some(request.body),
            serviceResponse.correlationId,
            AuditResponse(httpStatus = OK, response = Right(Some(jsonResponse)))
          )
        )

        Ok(jsonResponse)
          .withApiHeaders(serviceResponse.correlationId)
      }

      result.leftMap { errorWrapper =>
        val resCorrelationId = errorWrapper.correlationId
        val result           = errorResult(errorWrapper).withApiHeaders(resCorrelationId)
        logger.warn(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
            s"Error response received with CorrelationId: $resCorrelationId")

        auditSubmission(
          FlattenedGenericAuditDetail(
            Some("1.0"),
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
      case BadRequestError | NinoFormatError | TaxYearFormatError | RuleTaxYearRangeInvalidError | RuleTaxYearNotSupportedError | CustomMtdError(
            ValueFormatError.code) | CustomMtdError(RuleIncorrectOrEmptyBodyError.code) =>
        BadRequest(Json.toJson(errorWrapper))
      case NotFoundError           => NotFound(Json.toJson(errorWrapper))
      case StandardDownstreamError => InternalServerError(Json.toJson(errorWrapper))
      case _                       => unhandledError(errorWrapper)
    }

  private def auditSubmission(details: FlattenedGenericAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("CreateAndAmendUkDividendsIncome", "create-amend-uk-dividends-income", details)
    auditService.auditEvent(event)
  }

}