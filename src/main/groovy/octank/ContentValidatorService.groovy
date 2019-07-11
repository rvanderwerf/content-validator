package octank

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.comprehend.AmazonComprehend
import com.amazonaws.services.comprehend.AmazonComprehendClientBuilder
import com.amazonaws.services.comprehend.model.DetectSentimentRequest
import com.amazonaws.services.comprehend.model.DetectSentimentResult
import groovy.transform.CompileStatic

import javax.inject.Singleton

@Singleton
@CompileStatic
class ContentValidatorService {

     String hello() {
         "hello"
     }

}
