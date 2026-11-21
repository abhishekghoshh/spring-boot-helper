# Batch processing with Spring

> **Primer:** Spring Batch is Spring's chunk-oriented batch framework — it reads
> records in bulk, processes them, and writes them in transactional chunks, with
> restartability, skip/retry, and job metadata persisted in a `JobRepository`.
> Use it whenever a nightly file/DB job must survive crashes and resume where it
> stopped instead of restarting from row zero.

## Real-life use case

Nightly settlement: a 10M-row CSV of card transactions must be validated,
converted to settlement records, and written to Postgres. If the JVM dies at
row 6M, the job restarts at the last committed chunk — not from the beginning.

## Mental model

```text
JobLauncher → Job → Step (chunk-oriented)
                         ┌─────────────┐
  CSV rows → ItemReader → │ chunk(1000) │ → ItemProcessor → ItemWriter → DB
                         └──────┬──────┘
                                │ commit every 1000 rows
                                v
                         JobRepository (BATCH_* tables: status + ExecutionContext)
```

## Minimal example

```java
package com.example.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class SettlementJobConfig {

    @Bean
    public Job settlementJob(JobRepository jobs, Step chunkStep) {
        return new JobBuilder("settlementJob", jobs).start(chunkStep).build();
    }

    @Bean
    public Step chunkStep(JobRepository jobs, PlatformTransactionManager txm,
                          ItemReader<Order> reader,
                          ItemProcessor<Order, Settlement> processor,
                          ItemWriter<Settlement> writer) {
        return new StepBuilder("chunkStep", jobs)
                .<Order, Settlement>chunk(1000, txm) // commit every 1000 rows
                .reader(reader).processor(processor).writer(writer)
                .faultTolerant().skip(Exception.class).skipLimit(100)
                .build();
    }
}
```

```yaml
spring:
  batch:
    job:
      enabled: false        # don't auto-run on startup; launch via scheduler/API
    jdbc:
      initialize-schema: always   # creates BATCH_* metadata tables
```

## Pitfalls

- The `ItemReader` must be restart-safe (persist offset in `ExecutionContext`) or a restart re-reads from the top.
- Chunk size is a memory/transaction trade-off: too big → long TX + OOM; too small → commit overhead.
- Never run the job on every app startup in prod — gate with `spring.batch.job.enabled=false` and trigger explicitly.

## Interview Q&A

**Q1. Chunk-oriented step vs tasklet — when is each right?**
A: Chunks for bulk read-process-write over a dataset (streaming, commit per
chunk). Tasklets for single-shot operations (call a stored procedure, delete a
file, invoke an API once).

**Q2. How does a failed job resume instead of restarting?**
A: Each chunk commit persists offset + status in `JobRepository` (`BATCH_*`
tables). On relaunch with the same job parameters, Spring reads the last
`ExecutionContext` and the reader continues after the last committed offset.

---


## Udemy

- [Batch Processing with Spring Batch & Spring Boot](https://www.udemy.com/course/data-batch-processing-with-spring-batch-spring-boot-spring-framework/)