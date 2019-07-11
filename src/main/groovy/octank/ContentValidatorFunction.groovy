package octank

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.comprehend.AmazonComprehend
import com.amazonaws.services.comprehend.AmazonComprehendClientBuilder
import com.amazonaws.services.comprehend.model.DetectDominantLanguageRequest
import com.amazonaws.services.comprehend.model.DetectDominantLanguageResult
import com.amazonaws.services.comprehend.model.DetectEntitiesRequest
import com.amazonaws.services.comprehend.model.DetectEntitiesResult
import com.amazonaws.services.comprehend.model.DetectKeyPhrasesRequest
import com.amazonaws.services.comprehend.model.DetectKeyPhrasesResult
import com.amazonaws.services.comprehend.model.DetectSentimentRequest
import com.amazonaws.services.comprehend.model.DetectSentimentResult
import com.amazonaws.services.comprehend.model.DominantLanguage
import com.amazonaws.services.comprehend.model.Entity

import javax.inject.Inject
import groovy.transform.Field




@Field @Inject
ContentValidatorService contentValidatorService

/**
 * this is the main entry point
 * @param cmd
 * @return
 */
ComprehendResults validate(ValidateCommand cmd) {
    if (!cmd.division) {
        System.out.println('No division given defaulting to travel')
        cmd.division = "Travel" // default to travel
    }
       System.out.println("text=${cmd.text} ")
      if (!contentValidatorService) {
          System.out.println("Error - ContentValidatorService is NOT injected!!")
      }
      ComprehendResults results = detectAll(cmd.text,cmd.division)
      results
}



private Sentiment detectSentiment(String text, String division) {
    AWSCredentialsProvider awsCreds = DefaultAWSCredentialsProviderChain.getInstance()

    AmazonComprehend comprehendClient =
            AmazonComprehendClientBuilder.standard()
                    .withCredentials(awsCreds)
                    .withRegion("us-east-1")
                    .build()

    // Call detectSentiment API
    System.out.println("Calling DetectSentiment")
    DetectSentimentRequest detectSentimentRequest = new DetectSentimentRequest().withText(text)
            .withLanguageCode("en")
    DetectSentimentResult detectSentimentResult = comprehendClient.detectSentiment(detectSentimentRequest)

    Sentiment sentiment = new Sentiment()
    sentiment.scoreMixed = detectSentimentResult.sentimentScore.mixed
    sentiment.scoreNegative = detectSentimentResult.sentimentScore.negative
    sentiment.scorePositive = detectSentimentResult.sentimentScore.positive
    sentiment.sentiment =  detectSentimentResult.sentiment
    if (sentiment.sentiment == "NEUTRAL" && sentiment.scorePositive.round(3) > 0) {
        sentiment.sentiment = "POSITIVE"
    }
    if (sentiment.sentiment == "NEUTRAL" && sentiment.scoreNegative.round(3) > 0) {
        sentiment.sentiment = "NEGATIVE"
    }

    sentiment

}


private Entities detectEntities(String text, String division) {
    List<octank.Entity> entities = [] as List<octank.Entity>
    Entities returnEntities = new Entities()
    returnEntities.entities = entities
    AWSCredentialsProvider awsCreds = DefaultAWSCredentialsProviderChain.getInstance();

    AmazonComprehend comprehendClient =
            AmazonComprehendClientBuilder.standard()
                    .withCredentials(awsCreds)
                    .withRegion("us-east-1")
                    .build()

    // Call detectEntities API
    System.out.println("Calling DetectEntities")
    DetectEntitiesRequest detectEntitiesRequest = new DetectEntitiesRequest().withText(text)
            .withLanguageCode("en")
    DetectEntitiesResult detectEntitiesResult  = comprehendClient.detectEntities(detectEntitiesRequest);
    boolean hasPlace = false
    boolean hasPerson = false
    boolean hasQuantity = false

    detectEntitiesResult.getEntities().each() { Entity awsEntity ->
        octank.Entity entity = new octank.Entity()
        entity.beginOffset = awsEntity.beginOffset
        entity.endOffset = awsEntity.endOffset
        entity.entityType = awsEntity.type
        if (entity.entityType.toLowerCase() == "location") {
            hasPlace = true
        }
        if (entity.entityType.toLowerCase() == "person") {
            hasPerson = true
        }
        if (entity.entityType.toLowerCase() == "quantity")
        entity.text = awsEntity.text
        entity.score = awsEntity.score
        entities.add(entity)
    }
    if (division.toLowerCase() == "travel") {
        if (!hasPlace) {
           returnEntities.warnings = "No locations detected for travel!"
        }

    }
    if (division.toLowerCase() == "energy" || division.toLowerCase() == "health") {
        if (!hasQuantity) {
            returnEntities.warnings = "No quantities or numbers detected!"
        }
    }
    if (division.toLowerCase() == "fashion") {
        if (!hasPerson) {
            returnEntities.warnings = "No people listed for fashion!"
        }
    }

    System.out.println("End of DetectEntities\n")
    returnEntities
}

private KeyPhrases detectKeyPhrases(String text, String division) {
    List<KeyPhrase> keyPhrases = [] as List<KeyPhrase>
    AWSCredentialsProvider awsCreds = DefaultAWSCredentialsProviderChain.getInstance();

    AmazonComprehend comprehendClient =
            AmazonComprehendClientBuilder.standard()
                    .withCredentials(awsCreds)
                    .withRegion("us-east-1")
                    .build()

    // Call detectKeyPhrases API
    System.out.println("Calling DetectKeyPhrases");
    DetectKeyPhrasesRequest detectKeyPhrasesRequest = new DetectKeyPhrasesRequest().withText(text)
            .withLanguageCode("en")
    DetectKeyPhrasesResult detectKeyPhrasesResult = comprehendClient.detectKeyPhrases(detectKeyPhrasesRequest)
    detectKeyPhrasesResult.getKeyPhrases().each { com.amazonaws.services.comprehend.model.KeyPhrase awsKeyPhrase ->
        KeyPhrase keyPhrase = new KeyPhrase()
        keyPhrase.score = awsKeyPhrase.score
        keyPhrase.text = awsKeyPhrase.text
        keyPhrase.beginOffset = awsKeyPhrase.beginOffset
        keyPhrase.endOffset = awsKeyPhrase.endOffset
        keyPhrases.add(keyPhrase)
    }
    System.out.println("End of DetectKeyPhrases\n")
    KeyPhrases returnKeyPhrases = new KeyPhrases()
    returnKeyPhrases.keyPhrases = keyPhrases
    returnKeyPhrases
}

private Languages detectLanguages(String text, String division) {
    Languages returnLangs = new Languages()
    List<Language> languages = [] as List<Language>
    AWSCredentialsProvider awsCreds = DefaultAWSCredentialsProviderChain.getInstance()

    AmazonComprehend comprehendClient =
            AmazonComprehendClientBuilder.standard()
                    .withCredentials(awsCreds)
                    .withRegion("us-east-1")
                    .build()

    // Call detectDominantLanguage API
    System.out.println("Calling DetectDominantLanguage")
    DetectDominantLanguageRequest detectDominantLanguageRequest = new DetectDominantLanguageRequest().withText(text);
    DetectDominantLanguageResult detectDominantLanguageResult = comprehendClient.detectDominantLanguage(detectDominantLanguageRequest);
    detectDominantLanguageResult.getLanguages().each { DominantLanguage dominantLanguage ->
        Language language = new Language()
        language.score = dominantLanguage.score
        language.languageCode = dominantLanguage.languageCode
        languages.add(language)
    }
    System.out.println("Calling DetectDominantLanguage\n")
    System.out.println("Done")
    returnLangs.languages = languages
    returnLangs

}

private ComprehendResults detectAll(String text, String division) {
    ComprehendResults comprehendResults = new ComprehendResults()
    if (text?.length() > 5000) {
       comprehendResults.warnings = "Warning text is over 5000 chars, truncating!"
        text =  text.substring(0,4999)
    }

    Sentiment sentiment = detectSentiment(text,division)
    comprehendResults.sentiment = sentiment
    comprehendResults.entities = detectEntities(text,division)
    comprehendResults.keyPhrases = detectKeyPhrases(text,division)
    comprehendResults.languages = detectLanguages(text,division)
    comprehendResults
}

