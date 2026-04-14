package io.github.abhishekghoshh.aws.dynamodb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDBConfig {

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean("dynamoDbClient")
    @Profile("local")
    public DynamoDbEnhancedClient localDynamoDbClient(@Value("${cloud.aws.credentials.accessKey}") String accessKey,
                                                      @Value("${cloud.aws.credentials.secretKey}") String secretKey) {
        // Create a DynamoDbClient with static credentials for local development
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();

        // Create and return the DynamoDbEnhancedClient using the DynamoDbClient
        // The DynamoDbEnhancedClient provides Object Mapping and higher-level APIs for working with DynamoDB
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

    }

    @Bean("dynamoDbClient")
    @Profile("dev")
    public DynamoDbEnhancedClient devDynamoDbClient() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();

        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
