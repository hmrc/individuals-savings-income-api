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

package v2.listUkSavingsAccounts.model.response

import api.utils.UnitSpec
import play.api.libs.json.{JsError, Json}

class ListUkSavingsAccountsResponseSpec extends UnitSpec {

  private val ukSavingsAccountsDownstreamJson = Json.parse(
    """
      |{
      |  "bbsi": [
      |      {
      |        "incomeSourceId": "000000000000001",
      |        "incomeSourceName": "Bank Account 1",
      |        "startDate": "2024-08-22",
      |        "endDate": "2024-08-22"
      |      },
      |      {
      |        "incomeSourceId": "000000000000002",
      |        "incomeSourceName": "Bank Account 2",
      |        "startDate": "2024-08-22",
      |        "endDate": "2024-08-22"
      |      },
      |      {
      |        "incomeSourceId": "000000000000003"
      |      }
      |  ]
      |}
    """.stripMargin
  )

  private val ukSavingsAccountsNotBbsiDownstreamJson = Json.parse(
    """
      |{
      |  "not_bbsi": [
      |      {
      |        "incomeSourceId": "000000000000003"
      |      }
      |  ]
      |}
    """.stripMargin
  )

  private val ukSavingsAccountsMtdJson = Json.parse(
    """
      |{
      |  "savingsAccounts": [
      |    {
      |        "savingsAccountId": "000000000000001",
      |        "accountName": "Bank Account 1"
      |    },
      |    {
      |        "savingsAccountId": "000000000000002",
      |        "accountName": "Bank Account 2"
      |    },
      |    {
      |        "savingsAccountId": "000000000000003"
      |    }
      |  ]
      |}
    """.stripMargin
  )

  private val emptyBbsiJson = Json.parse(
    """
      |{
      |  "bbsi": []
      |}
    """.stripMargin
  )

  private val listUkSavingsAccountResponse = ListUkSavingsAccountsResponse(
    Some(
      List(
        UkSavingsAccount("000000000000001", Some("Bank Account 1")),
        UkSavingsAccount("000000000000002", Some("Bank Account 2")),
        UkSavingsAccount("000000000000003", None)
      )
    )
  )

  "ListUkSavingsAccountsResponse" when {
    ".reads" should {
      "return a parsed ListUkSavingsAccountsResponse" when {
        "given a valid JSON document from downstream" in {
          val result = ukSavingsAccountsDownstreamJson.as[ListUkSavingsAccountsResponse]
          result shouldBe listUkSavingsAccountResponse
        }
      }

      "return a JSON error" when {
        "given a downstream response that does not include bbsi" in {
          val result = ukSavingsAccountsNotBbsiDownstreamJson.validate[ListUkSavingsAccountsResponse]
          result shouldBe a[JsError]
        }
      }

      "return a valid empty ListUkSavingsAccountsResponse model" when {
        "given a valid empty uk savings account list json from downstream" in {
          val result = emptyBbsiJson.as[ListUkSavingsAccountsResponse]
          result shouldBe ListUkSavingsAccountsResponse(None)
        }
      }
    }

    ".writes" should {
      "return a valid list uk savings account response MTD json" when {
        "given a valid ListUkSavingsAccountsResponse" in {
          val result = Json.toJson(listUkSavingsAccountResponse)
          result shouldBe ukSavingsAccountsMtdJson
        }
      }
    }
  }

}
