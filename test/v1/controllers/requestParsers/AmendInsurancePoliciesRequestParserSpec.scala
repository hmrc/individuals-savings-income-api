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

package v1.controllers.requestParsers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v1.mocks.validators.MockAmendInsurancePoliciesValidator
import v1.models.domain.DesTaxYear
import v1.models.errors._
import v1.models.request.insurancePolicies.amend._

class AmendInsurancePoliciesRequestParserSpec extends UnitSpec{

  val nino: String = "AA123456B"
  val taxYear: String = "2017-18"

  private val validRequestBodyJson: JsValue = Json.parse(
    """
      |{
      |   "lifeInsurance":[
      |       {
      |           "customerReference": "INPOLY123A",
      |           "event": "Death of spouse",
      |           "gainAmount": 2000.99,
      |           "taxPaid": 5000.99,
      |           "yearsHeld": 15,
      |           "yearsHeldSinceLastGain": 12,
      |           "deficiencyRelief": 5000.99
      |       },
      |       {
      |           "customerReference": "INPOLY123A",
      |           "event": "Death of spouse",
      |           "gainAmount": 2000.99,
      |           "taxPaid": 5000.99,
      |           "yearsHeld": 15,
      |           "yearsHeldSinceLastGain": 12,
      |           "deficiencyRelief": 5000.99
      |       }
      |   ],
      |   "capitalRedemption":[
      |       {
      |           "customerReference": "INPOLY123A",
      |           "event": "Death of spouse",
      |           "gainAmount": 2000.99,
      |           "taxPaid": 5000.99,
      |           "yearsHeld": 15,
      |           "yearsHeldSinceLastGain": 12,
      |           "deficiencyRelief": 5000.99
      |       },
      |       {
      |           "customerReference": "INPOLY123A",
      |           "event": "Death of spouse",
      |           "gainAmount": 2000.99,
      |           "taxPaid": 5000.99,
      |           "yearsHeld": 15,
      |           "yearsHeldSinceLastGain": 12,
      |           "deficiencyRelief": 5000.99
      |       }
      |   ],
      |   "lifeAnnuity":[
      |       {
      |           "customerReference": "INPOLY123A",
      |           "event": "Death of spouse",
      |           "gainAmount": 2000.99,
      |           "taxPaid": 5000.99,
      |           "yearsHeld": 15,
      |           "yearsHeldSinceLastGain": 12,
      |           "deficiencyRelief": 5000.99
      |       },
      |       {
      |           "customerReference": "INPOLY123A",
      |           "event": "Death of spouse",
      |           "gainAmount": 2000.99,
      |           "taxPaid": 5000.99,
      |           "yearsHeld": 15,
      |           "yearsHeldSinceLastGain": 12,
      |           "deficiencyRelief": 5000.99
      |       }
      |   ],
      |   "voidedIsa":[
      |       {
      |           "customerReference": "INPOLY123A",
      |           "event": "Death of spouse",
      |           "gainAmount": 2000.99,
      |           "taxPaid": 5000.99,
      |           "yearsHeld": 15,
      |           "yearsHeldSinceLastGain": 12
      |       },
      |       {
      |           "customerReference": "INPOLY123A",
      |           "event": "Death of spouse",
      |           "gainAmount": 2000.99,
      |           "taxPaid": 5000.99,
      |           "yearsHeld": 15,
      |           "yearsHeldSinceLastGain": 12
      |       }
      |   ],
      |   "foreign":[
      |       {
      |           "customerReference": "INPOLY123A",
      |           "gainAmount": 2000.99,
      |           "taxPaid": 5000.99,
      |           "yearsHeld": 15
      |       },
      |       {
      |           "customerReference": "INPOLY123A",
      |           "gainAmount": 2000.99,
      |           "taxPaid": 5000.99,
      |           "yearsHeld": 15
      |       }
      |   ]
      |}
    """.stripMargin
  )

  private val validRawRequestBody = AnyContentAsJson(validRequestBodyJson)

  private val fullLifeInsuranceModel = LifeInsurance(
    customerReference = "INPOLY123A",
    event = Some("Death of spouse"),
    gainAmount = Some(2000.99),
    taxPaid = Some(5000.99),
    yearsHeld = Some(15),
    yearsHeldSinceLastGain = Some(12),
    deficiencyRelief = Some(5000.99)
  )

  private val fullCapitalRedemptionModel = CapitalRedemption(
    customerReference = "INPOLY123A",
    event = Some("Death of spouse"),
    gainAmount = Some(2000.99),
    taxPaid = Some(5000.99),
    yearsHeld = Some(15),
    yearsHeldSinceLastGain = Some(12),
    deficiencyRelief = Some(5000.99)
  )

  private val fullLifeAnnuityModel = LifeAnnuity(
    customerReference = "INPOLY123A",
    event = Some("Death of spouse"),
    gainAmount = Some(2000.99),
    taxPaid = Some(5000.99),
    yearsHeld = Some(15),
    yearsHeldSinceLastGain = Some(12),
    deficiencyRelief = Some(5000.99)
  )

  private val fullVoidedIsaModel = VoidedIsa(
    customerReference = "INPOLY123A",
    event = Some("Death of spouse"),
    gainAmount = Some(2000.99),
    taxPaid = Some(5000.99),
    yearsHeld = Some(15),
    yearsHeldSinceLastGain = Some(12)
  )

  private  val fullForeignModel = Foreign(
    customerReference = "INPOLY123A",
    gainAmount = Some(2000.99),
    taxPaid = Some(5000.99),
    yearsHeld = Some(15)
  )

  private val validRequestBodyModel = AmendRequestBody(
    lifeInsurance = Some(Seq(fullLifeInsuranceModel, fullLifeInsuranceModel)),
    capitalRedemption = Some(Seq(fullCapitalRedemptionModel, fullCapitalRedemptionModel)),
    lifeAnnuity = Some(Seq(fullLifeAnnuityModel, fullLifeAnnuityModel)),
    voidedIsa = Some(Seq(fullVoidedIsaModel, fullVoidedIsaModel)),
    foreign = Some(Seq(fullForeignModel, fullForeignModel))
  )

  private val amendInsurancePoliciesRawData = AmendRawData(
    nino = nino,
    taxYear = taxYear,
    body = validRawRequestBody
  )

  trait Test extends MockAmendInsurancePoliciesValidator {
    lazy val parser: AmendInsurancePoliciesRequestParser = new AmendInsurancePoliciesRequestParser(
      validator = mockAmendInsurancePoliciesValidator
    )
  }

  "parse" should {
    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockAmendInsurancePoliciesValidator.validate(amendInsurancePoliciesRawData).returns(Nil)

        parser.parseRequest(amendInsurancePoliciesRawData) shouldBe
          Right(AmendRequest(Nino(nino), DesTaxYear.fromMtd(taxYear), validRequestBodyModel))
      }
    }

    "return an ErrorWrapper" when {
      "a single validation error occurs" in new Test {
        MockAmendInsurancePoliciesValidator.validate(amendInsurancePoliciesRawData.copy(nino = "notANino"))
          .returns(List(NinoFormatError))

        parser.parseRequest(amendInsurancePoliciesRawData.copy(nino = "notANino")) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "multiple path parameter validation errors occur" in new Test {
        MockAmendInsurancePoliciesValidator.validate(amendInsurancePoliciesRawData.copy(nino = "notANino", taxYear = "notATaxYear"))
          .returns(List(NinoFormatError, TaxYearFormatError))

        parser.parseRequest(amendInsurancePoliciesRawData.copy(nino = "notANino", taxYear = "notATaxYear")) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }

      "multiple field value validation errors occur" in new Test {

        private val allInvalidValueRequestBodyJson: JsValue = Json.parse(
          """
            {
            |   "lifeInsurance":[
            |       {
            |           "customerReference": "This ref is more than 25 characters",
            |           "event": "Death of spouse",
            |           "gainAmount": 2000.999,
            |           "taxPaid": 5000.999,
            |           "yearsHeld": -15,
            |           "yearsHeldSinceLastGain": 12,
            |           "deficiencyRelief": 5000.999
            |       },
            |       {
            |           "customerReference": "INPOLY123A",
            |           "event": "This event string is 76 characters long --------------------------------- 76",
            |           "gainAmount": 2000.99,
            |           "taxPaid": 5000.99,
            |           "yearsHeld": 15,
            |           "yearsHeldSinceLastGain": 12,
            |           "deficiencyRelief": 5000.99
            |       }
            |   ],
            |   "capitalRedemption":[
            |       {
            |           "customerReference": "This ref is more than 25 characters",
            |           "event": "Death of spouse",
            |           "gainAmount": 3000.999,
            |           "taxPaid": 5000.99,
            |           "yearsHeld": -15,
            |           "yearsHeldSinceLastGain": 12,
            |           "deficiencyRelief": 5000.99
            |       },
            |       {
            |           "customerReference": "INPOLY123A",
            |           "event": "Death of spouse",
            |           "gainAmount": 2000.99,
            |           "taxPaid": 5000.999,
            |           "yearsHeld": 15,
            |           "yearsHeldSinceLastGain": 120,
            |           "deficiencyRelief": 5000.999
            |       }
            |   ],
            |   "lifeAnnuity":[
            |       {
            |           "customerReference": "INPOLY123A",
            |           "event": "Death of spouse",
            |           "gainAmount": 2000.99,
            |           "taxPaid": 5000.999,
            |           "yearsHeld": -15,
            |           "yearsHeldSinceLastGain": 12,
            |           "deficiencyRelief": 5000.999
            |       },
            |       {
            |           "customerReference": "This ref is more than 25 characters",
            |           "event": "This event string is 76 characters long --------------------------------- 76",
            |           "gainAmount": 5000.99,
            |           "taxPaid": 5000.99,
            |           "yearsHeld": 15,
            |           "yearsHeldSinceLastGain": 12,
            |           "deficiencyRelief": 5000.99
            |       }
            |   ],
            |   "voidedIsa":[
            |       {
            |           "customerReference": "INPOLY123A",
            |           "event": "Death of spouse",
            |           "gainAmount": 2000.99,
            |           "taxPaid": 5000.99,
            |           "yearsHeld": -15,
            |           "yearsHeldSinceLastGain": 120
            |       },
            |       {
            |           "customerReference": "This ref is more than 25 characters",
            |           "event": "Death of spouse",
            |           "gainAmount": 5000.999,
            |           "taxPaid": 5000.999,
            |           "yearsHeld": 15,
            |           "yearsHeldSinceLastGain": 12
            |       }
            |   ],
            |   "foreign":[
            |       {
            |           "customerReference": "This ref is more than 25 characters",
            |           "gainAmount": 5000.99,
            |           "taxPaid": 5000.999,
            |           "yearsHeld": 15
            |       },
            |       {
            |           "customerReference": "INPOLY123A",
            |           "gainAmount": 2000.999,
            |           "taxPaid": 5000.99,
            |           "yearsHeld": -15
            |       }
            |   ]
            |}
          """.stripMargin
        )

        private val allInvalidValueRawRequestBody = AnyContentAsJson(allInvalidValueRequestBodyJson)

        private val allInvalidValueErrors = List(
          CustomerRefFormatError.copy(
            paths = Some(List(
              "/lifeInsurance/0/customerReference",
              "/capitalRedemption/0/customerReference",
              "/lifeAnnuity/1/customerReference",
              "/voidedIsa/1/customerReference",
              "/foreign/0/customerReference"
            ))
          ),
          ValueFormatError.copy(
            message = "The field should be between 0.01 and 99999999999.99",
            paths = Some(List(
              "/lifeInsurance/0/gainAmount",
              "/lifeInsurance/0/deficiencyRelief",
              "/capitalRedemption/0/gainAmount",
              "/capitalRedemption/1/deficiencyRelief",
              "/lifeAnnuity/0/deficiencyRelief",
              "/foreign/1/gainAmount"
            ))
          ),
          EventFormatError.copy(
            paths = Some(List(
              "/lifeInsurance/1/event",
              "/lifeAnnuity/1/event"
            ))
          ),
          ValueFormatError.copy(
            message = "The field should be between 0 and 99",
            paths = Some(List(
              "/lifeInsurance/0/yearsHeld",
              "/capitalRedemption/0/yearsHeld",
              "/capitalRedemption/1/yearsHeldSinceLastGain",
              "/lifeAnnuity/0/yearsHeld",
              "/voidedIsa/0/yearsHeld",
              "/voidedIsa/0/yearsHeldSinceLastGain",
              "/foreign/1/yearsHeld"
            ))
          ),
          ValueFormatError.copy(
            message = "The field should be between 0 and 99999999999.99",
            paths = Some(List(
              "/lifeInsurance/0/taxPaid",
              "/capitalRedemption/1/taxPaid",
              "/lifeAnnuity/0/taxPaid",
              "/voidedIsa/1/gainAmount",
              "/voidedIsa/1/taxPaid",
              "/foreign/0/taxPaid",
            ))
          )
        )

        MockAmendInsurancePoliciesValidator.validate(amendInsurancePoliciesRawData.copy(body = allInvalidValueRawRequestBody))
          .returns(allInvalidValueErrors)

        parser.parseRequest(amendInsurancePoliciesRawData.copy(body = allInvalidValueRawRequestBody)) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(allInvalidValueErrors)))
      }
    }
  }
}