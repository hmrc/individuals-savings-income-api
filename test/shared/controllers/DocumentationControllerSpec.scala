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

package shared.controllers

import com.typesafe.config.ConfigFactory
import controllers.{AssetsConfiguration, DefaultAssetsMetadata, RewriteableAssets}
import play.api.{Configuration, Environment}
import play.api.http.{DefaultFileMimeTypes, DefaultHttpErrorHandler, FileMimeTypesConfiguration, HttpConfiguration}
import play.api.mvc.Result
import shared.config.rewriters._
import shared.config.{AppConfig, MockAppConfig}
import shared.definition._
import shared.routing.Version1
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DocumentationControllerSpec extends ControllerBaseSpec with MockAppConfig {
  // TODO: Update this test so that its not specific to the individuals-savings-income-api

  private val apiTitle = "Individuals Savings Income (MTD)"

  "/file endpoint" should {
    "return a file" in new Test {
      MockedAppConfig.apiVersionReleasedInProduction("1.0").anyNumberOfTimes() returns true
      MockedAppConfig.endpointsEnabled("1.0").anyNumberOfTimes() returns true
      val response: Future[Result] = requestAsset("application.yaml")
      status(response) shouldBe OK
      await(response).body.contentLength.getOrElse(-99L) should be > 0L
    }
  }

  "rewrite()" when {
    "the API version is disabled" should {
      "return the yaml with [test only] in the API title" in new Test {
        MockedAppConfig.apiVersionReleasedInProduction("1.0").anyNumberOfTimes() returns false
        MockedAppConfig.endpointsEnabled("1.0").anyNumberOfTimes() returns true

        val response: Future[Result] = requestAsset("application.yaml")
        status(response) shouldBe OK

        private val result = contentAsString(response)
        result should include(s"""  title: "$apiTitle [test only]"""")

        withClue("Only the title should have [test only] appended:") {
          numberOfTestOnlyOccurrences(result) shouldBe 1
        }

        result should startWith("""openapi: "3.0.3"
                                  |
                                  |info:
                                  |  version: "1.0"""".stripMargin)
      }
    }
    "the API version is enabled" should {
      "return the yaml with the API title unchanged" in new Test {
        MockedAppConfig.apiVersionReleasedInProduction("1.0").anyNumberOfTimes() returns true
        MockedAppConfig.endpointsEnabled("1.0").anyNumberOfTimes() returns true

        val response: Future[Result] = requestAsset("application.yaml", accept = "text/plain")
        status(response) shouldBe OK

        private val result = contentAsString(response)

        result should include(s"""  title: $apiTitle""")
        numberOfTestOnlyOccurrences(result) shouldBe 0

        result should startWith("""openapi: "3.0.3"
                                  |
                                  |info:
                                  |  version: "1.0"""".stripMargin)
      }
    }
  }

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    protected def featureEnabled: Boolean = true

    protected def requestAsset(filename: String, accept: String = "text/yaml"): Future[Result] =
      controller.asset("1.0", filename)(fakeGetRequest.withHeaders(ACCEPT -> accept))

    protected def numberOfTestOnlyOccurrences(str: String): Int = "\\[test only]".r.findAllIn(str).size

    MockedAppConfig.featureSwitchConfig returns Configuration("openApiFeatureTest.enabled" -> featureEnabled)

    private val apiFactory = new ApiDefinitionFactory {
      protected val appConfig: AppConfig = mockAppConfig

      val definition: Definition = Definition(
        APIDefinition(
          "test API definition",
          "description",
          "context",
          List("category"),
          List(APIVersion(Version1, APIStatus.BETA, endpointsEnabled = true)),
          None)
      )

    }

    private val config    = new Configuration(ConfigFactory.load())
    private val mimeTypes = HttpConfiguration.parseFileMimeTypes(config) ++ Map("yaml" -> "text/yaml", "md" -> "text/markdown")

    private val assetsMetadata =
      new DefaultAssetsMetadata(
        AssetsConfiguration(textContentTypes = Set("text/yaml", "text/markdown")),
        path => {
          Option(getClass.getResource(path))
        },
        new DefaultFileMimeTypes(FileMimeTypesConfiguration(mimeTypes))
      )

    private val errorHandler = new DefaultHttpErrorHandler()

    private val docRewriters = new DocumentationRewriters(
      new ApiVersionTitleRewriter(mockAppConfig),
      new EndpointSummaryRewriter(mockAppConfig),
      new EndpointSummaryGroupRewriter(mockAppConfig),
      new OasFeatureRewriter()(mockAppConfig)
    )

    private val assets       = new RewriteableAssets(errorHandler, assetsMetadata, mock[Environment])
    protected val controller = new DocumentationController(apiFactory, docRewriters, assets, cc)
  }

}
