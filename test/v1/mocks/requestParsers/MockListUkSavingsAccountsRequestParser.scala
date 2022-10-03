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

package v1.mocks.requestParsers

import api.models.errors.ErrorWrapper
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import v1.models.request.listUkSavingsAccounts.{ListUkSavingsAccountsRawData, ListUkSavingsAccountsRequest}
import v1.requestParsers.ListUkSavingsAccountsRequestParser

trait MockListUkSavingsAccountsRequestParser extends MockFactory {

  val mockListUkSavingsAccountsRequestParser: ListUkSavingsAccountsRequestParser = mock[ListUkSavingsAccountsRequestParser]

  object MockListUkSavingsAccountsRequestParser {

    def parse(data: ListUkSavingsAccountsRawData): CallHandler[Either[ErrorWrapper, ListUkSavingsAccountsRequest]] = {
      (mockListUkSavingsAccountsRequestParser.parseRequest(_: ListUkSavingsAccountsRawData)(_: String)).expects(data, *)
    }

  }

}