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

package v2.retrieveSavings.def1.model.response

import play.api.libs.json.{JsError, JsValue, Json}
import shared.models.domain.Timestamp
import shared.utils.UnitSpec

class Def1_RetrieveSavingsResponseSpec extends UnitSpec {

  private val desResponse: JsValue = Json.parse(
    """
      |{
      |   "submittedOn": "2019-04-04T01:01:01Z",
      |   "securities":
      |      {
      |         "taxTakenOff": 100.0,
      |         "grossAmount": 1455.0,
      |         "netAmount": 123.22
      |      },
      |   "foreignInterest": [
      |      {
      |         "amountBeforeTax": 1232.22,
      |         "countryCode": "DEU",
      |         "taxTakenOff": 22.22,
      |         "specialWithholdingTax": 22.22,
      |         "taxableAmount": 2321.22,
      |         "foreignTaxCreditRelief": true
      |      }
      |   ]
      |}
    """.stripMargin
  )

  private val securitiesModel: Securities =
    Securities(
      taxTakenOff = Some(100.0),
      grossAmount = 1455.0,
      netAmount = Some(123.22)
    )

  private val foreignInterestsItemModel: ForeignInterestItem =
    ForeignInterestItem(
      amountBeforeTax = Some(1232.22),
      countryCode = "DEU",
      taxTakenOff = Some(22.22),
      specialWithholdingTax = Some(22.22),
      taxableAmount = 2321.22,
      foreignTaxCreditRelief = Some(true)
    )

  private val model: Def1_RetrieveSavingsResponse =
    Def1_RetrieveSavingsResponse(
      submittedOn = Timestamp("2019-04-04T01:01:01.000Z"),
      securities = Some(securitiesModel),
      foreignInterest = Some(Seq(foreignInterestsItemModel))
    )

  private val minimumSecuritiesModel: Securities =
    Securities(
      taxTakenOff = None,
      grossAmount = 1499.99,
      netAmount = None
    )

  private val minimumForeignInterestsItemModel: ForeignInterestItem =
    ForeignInterestItem(
      amountBeforeTax = None,
      countryCode = "DEU",
      taxTakenOff = None,
      specialWithholdingTax = None,
      taxableAmount = 3000.33,
      foreignTaxCreditRelief = Some(true)
    )

  private val minimumModel: Def1_RetrieveSavingsResponse =
    Def1_RetrieveSavingsResponse(
      submittedOn = Timestamp("2019-04-04T01:01:01.000Z"),
      securities = Some(minimumSecuritiesModel),
      foreignInterest = Some(Seq(minimumForeignInterestsItemModel))
    )

  val mtdJson: JsValue = Json.parse(
    """
      |{
      |   "submittedOn": "2019-04-04T01:01:01.000Z",
      |   "securities":
      |      {
      |         "taxTakenOff": 100.0,
      |         "grossAmount": 1455.0,
      |         "netAmount": 123.22
      |      },
      |   "foreignInterest": [
      |      {
      |         "countryCode": "DEU",
      |         "amountBeforeTax": 1232.22,
      |         "taxTakenOff": 22.22,
      |         "specialWithholdingTax": 22.22,
      |         "foreignTaxCreditRelief": true,
      |         "taxableAmount": 2321.22
      |      }
      |   ]
      |}
    """.stripMargin
  )

  private val desResponseInvalid: JsValue = Json.parse(
    """
      |{
      |   "submittedOn": "2019-04-04T01:01:01Z",
      |   "securities":
      |      {
      |         "taxTakenOff": "abc",
      |         "grossAmount": 1455.0,
      |         "netAmount": 123.22
      |      },
      |   "foreignInterest": [
      |      {
      |         "amountBeforeTax": 1232.22,
      |         "countryCode": "DEU",
      |         "taxTakenOff": 22.22,
      |         "specialWithholdingTax": 22.22,
      |         "taxableAmount": 2321.22,
      |         "foreignTaxCreditRelief": true
      |      }
      |   ]
      |}
    """.stripMargin
  )

  val minimumFieldsJson: JsValue = Json.parse(
    """
      |{
      |   "submittedOn": "2019-04-04T01:01:01Z",
      |    "securities":
      |      {
      |         "grossAmount": 1499.99
      |      },
      |   "foreignInterest": [
      |      {
      |         "countryCode": "DEU",
      |         "taxableAmount": 3000.33,
      |         "foreignTaxCreditRelief": true
      |      }
      |   ]
      |}
    """.stripMargin
  )

  "RetrieveSavingsResponse" when {
    "read from valid JSON" should {
      "produce the expected RetrieveSavingsResponse model" in {
        desResponse.as[Def1_RetrieveSavingsResponse] shouldBe model
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        desResponseInvalid.validate[Def1_RetrieveSavingsResponse] shouldBe a[JsError]
      }
    }

    "read from a JSON with only mandatory fields" should {
      "produce the expected RetrieveSavingsResponse model" in {
        minimumFieldsJson.as[Def1_RetrieveSavingsResponse] shouldBe minimumModel
      }
    }

    "written to JSON" should {
      "produce the expected JSON" in {
        Json.toJson(model) shouldBe mtdJson
      }
    }
  }

}
