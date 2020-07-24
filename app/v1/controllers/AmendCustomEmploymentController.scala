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

package v1.controllers

import cats.data.EitherT
import cats.implicits._
import config.AppConfig
import javax.inject.Inject
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import play.mvc.Http.MimeTypes
import utils.Logging
import v1.controllers.requestParsers.AmendCustomEmploymentRequestParser
import v1.hateoas.AmendHateoasBody
import v1.models.errors._
import v1.models.request.amendCustomEmployment.AmendCustomEmploymentRawData
import v1.services.{AmendCustomEmploymentService, EnrolmentsAuthService, MtdIdLookupService}

import scala.concurrent.{ExecutionContext, Future}

class AmendCustomEmploymentController @Inject()(val authService: EnrolmentsAuthService,
                                                val lookupService: MtdIdLookupService,
                                                appConfig: AppConfig,
                                                requestParser: AmendCustomEmploymentRequestParser,
                                                service: AmendCustomEmploymentService,
                                                cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController with Logging with AmendHateoasBody {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(
      controllerName = "AmendCustomEmploymentController",
      endpointName = "amendCustomEmployment"
    )

  def amendEmployment(nino: String, taxYear: String, employmentId: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      val rawData: AmendCustomEmploymentRawData = AmendCustomEmploymentRawData(
        nino = nino,
        taxYear = taxYear,
        employmentId = employmentId,
        body = AnyContentAsJson(request.body)
      )

      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](requestParser.parseRequest(rawData))
          serviceResponse <- EitherT(service.amendEmployment(parsedRequest))
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          Ok(amendCustomEmploymentHateoasBody(appConfig, nino, taxYear, employmentId))
            .withApiHeaders(serviceResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result = errorResult(errorWrapper).withApiHeaders(correlationId)

        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError | NinoFormatError | TaxYearFormatError |
           EmploymentIdFormatError | StartDateFormatError | CessationDateFormatError |
           PayrollIdFormatError | RuleTaxYearNotSupportedError | RuleCessationDateBeforeStartDateError |
           RuleTaxYearRangeInvalidError | RuleIncorrectOrEmptyBodyError | RuleTaxYearNotEndedError |
           RuleStartDateAfterTaxYearEndError | RuleCessationDateBeforeTaxYearStartError |
           MtdErrorWithCustomMessage(EmployerNameFormatError.code) |
           MtdErrorWithCustomMessage(EmployerRefFormatError.code)
      => BadRequest(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }
}