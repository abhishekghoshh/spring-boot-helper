# Integration with Spring Boot

> **Primer:** Spring Cloud AWS maps AWS services onto Spring idioms: S3 via a
> `ResourceLoader` (`s3://bucket/key`), SQS via `@SqsListener`, SNS via
> notifications, DynamoDB via Spring Data repositories, and Secrets Manager /
> Parameter Store as property sources. Credentials come from the default chain
> (env → profile → IAM role), never hard-coded.

## Real-life use case

Order service on EKS with an IAM role: uploads invoices to S3 (presigned URLs
for the browser), consumes `order-events` from SQS with `@SqsListener`, stores
idempotency keys in DynamoDB, and reads DB passwords from Secrets Manager —
no static keys anywhere.

## Mental model

```text
  S3 .......... resourceLoader.getResource("s3://invoices/123.pdf")
  SQS ......... @SqsListener("order-events") → onMessage(payload)
  SNS ......... topic → fan-out to SQS queues / email / Lambda
  DynamoDB .... DynamoDbRepository<IdemKey, String> (like JPA, key-value)
  Secrets ..... spring.config.import=aws-secretsmanager:prod/db → ${db.password}
  Auth ........ IAM role for ServiceAccount (IRSA) — no access keys
```

## Minimal example

```java
package com.example.orders;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class OrderFiles {
    private final ResourceLoader resources;

    @Value("s3://invoices/")
    private Resource invoiceDir;

    public OrderFiles(ResourceLoader resources) { this.resources = resources; }

    @SqsListener("order-events")
    public void onOrder(OrderEvent event) throws Exception {
        Resource pdf = resources.getResource("s3://invoices/" + event.orderId() + ".pdf");
        // ... process, store idempotency key in DynamoDB, etc.
    }
}
```

```yaml
spring:
  config:
    import: aws-secretsmanager:prod/db   # ${db.password} available
  cloud:
    aws:
      region:
        static: ap-south-1
      credentials:
        profile:
          name: default                   # local only; prod uses IAM role
```

```xml
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-starter-s3</artifactId>
</dependency>
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-starter-sqs</artifactId>
</dependency>
```

## Pitfalls

- Presigned URLs (minutes-long expiry) for browser upload/download — never proxy multi-GB files through your pods.
- SQS visibility timeout must exceed handler time or the message is delivered twice — handlers must be idempotent regardless.
- LocalStack for local dev (`endpoint` override); never point dev code at real AWS with prod keys.

## Interview Q&A

**Q1. How does Spring Cloud AWS authentication work without keys in code?**
A: The AWS SDK default credentials chain: env vars → `~/.aws` profile (local)
→ ECS/EKS IAM role (prod via IRSA). The app just builds default clients;
Spring Cloud AWS wires region/credentials from properties, and prod pods
assume a scoped IAM role.

**Q2. SQS vs SNS vs EventBridge — which for what?**
A: SQS = queue (one consumer group, at-least-once, polling). SNS = fan-out
push to many subscribers. EventBridge = event bus with content-based routing
rules across accounts/services. Order events → EventBridge; work queues →
SQS; broadcast alerts → SNS.

---


## Official Documentation

- [Spring Cloud AWS](https://docs.awspring.io/spring-cloud-aws/docs/4.0.0/reference/html/index.html)
- [Set up an Apache Maven project that uses the AWS SDK for Java 2.x](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/setup-project-maven.html#sdk-as-dependency)
- [The official AWS SDK for Java - Version 2](https://github.com/aws/aws-sdk-java-v2/)



## Blogs

- [Integrating Amazon DynamoDB With Spring Boot Using Spring Cloud AWS](https://www.baeldung.com/spring-data-dynamodb)


- [Listing All AWS S3 Objects in a Bucket Using Java](https://www.baeldung.com/java-aws-s3-list-bucket-objects)
- [Working with Amazon S3 presigned URLs in Spring Boot](https://dev.to/aws-builders/working-with-amazon-s3-presigned-urls-in-spring-boot-383n)



## Medium

- [Spring Boot S3 Presigned Url Upload/Download File](https://medium.com/@gurkanucar/spring-boot-s3-presigned-url-upload-download-f-eff588eda7e3)
  - [gurkanucar/java-spring-learning/tree/master/aws-s3-file-service](https://github.com/gurkanucar/java-spring-learning/tree/master/aws-s3-file-service)
- [Spring Boot File Upload to S3 — The Efficient Way](https://medium.com/@ipla/spring-boot-file-upload-to-s3-the-efficient-way-6c119c8a08e8)
- [Presigned URLs: Secure Upload and Download on Amazon S3 with Java Spring Boot and NodeJS](https://blog.devgenius.io/presigned-urls-secure-upload-and-download-on-amazon-s3-with-java-spring-boot-and-nodejs-d0917e08e671)



## Youtube

- [AWS With Java Course](https://www.youtube.com/playlist?list=PL-bgVzzRdaPhE4h7u9OWe0cf7P6H-MpeM)
    - [codesnippetjava/S3DemoApplication](https://github.com/codesnippetjava/S3DemoApplication)


- [Spring Boot CRUD API with AWS DynamoDB | AWS DynamoDB](https://www.youtube.com/watch?v=LyOEmlu6Pqs)
    - [babysteps-topro/DynamoDBQueryScan](https://github.com/babysteps-topro/DynamoDBQueryScan)


- [AWS (Amazon Web Services)](https://www.youtube.com/playlist?list=PLVz2XdJiJQxxurKT1Dqz6rmiMuZNdClqv)

- [Spring Boot, React.js & AWS S3 Full Stack Development](https://www.youtube.com/watch?v=9i1gQ7w2V24)

- [Daily code buffer](https://www.youtube.com/@DailyCodeBuffer/playlists)
	- [How to Install and Configure AWS Command Line Interface (CLI) tool](https://www.youtube.com/watch?v=BzzCIsjrE7U)
	- [Dockerize Spring Boot App using Maven Plugin](https://www.youtube.com/watch?v=F_g44rliu6M)
	- [Create Docker Image without Dockerfile in Spring Boot](https://www.youtube.com/watch?v=pIei1m323KU)
	- [Spring Boot CRUD Example using AWS DynamoDB](https://www.youtube.com/watch?v=3ay92ZdCgwQ)
	- [How to Deploy Spring Boot Application with Docker into AWS EC2](https://www.youtube.com/watch?v=vmbhFZJUTR0)
	- [How to Deploy SpringBoot Application to AWS Elastic Beanstalk](https://www.youtube.com/watch?v=F9o21ydf6Wk)
	- [Deploy Spring Boot App in AWS EC2 instance using S3](https://www.youtube.com/watch?v=w7zDItQBo4c)
	- [Amazon Simple Queue Service (SQS) with SpringBoot | Spring Cloud AWS](https://www.youtube.com/watch?v=vYLlxxD_6LA)
	- [Amazon Simple Notification Service (SNS) with SpringBoot | Spring Cloud AWS](https://www.youtube.com/watch?v=9kXMLTNaQj8)