package com.community.springbootcommunitybatch.jobs.inactive;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.persistence.EntityManagerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import com.community.springbootcommunitybatch.domain.User;
import com.community.springbootcommunitybatch.domain.enums.Grade;
import com.community.springbootcommunitybatch.domain.enums.UserStatus;
import com.community.springbootcommunitybatch.jobs.inactive.listener.InactiveJobListener;
import com.community.springbootcommunitybatch.jobs.inactive.listener.InactiveStepListener;
import com.community.springbootcommunitybatch.jobs.readers.QueueItemReader;
import com.community.springbootcommunitybatch.repository.UserRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@AllArgsConstructor
public class InactiveUserJobConfig {

    private UserRepository userRepository;
    private final static int CHUNK_SIZE = 15;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job inactiveUserJob(JobBuilderFactory jobBuilderFactory, InactiveJobListener inactiveJobListener, Step partitionerStep) {
        return jobBuilderFactory.get("inactiveUserJob")
            .preventRestart()
            .listener(inactiveJobListener)
            // .start(inactiveJobStep)
            // .start(inactiveJobFlow)
            // .start(multiFlow)
            .start(partitionerStep)
            .build();
    }

    @Bean
    @JobScope
    public Step partitionerStep(StepBuilderFactory stepBuilderFactory, Step inactiveJobStep) {
        return stepBuilderFactory.get("partitionerStep")
            .partitioner("partitionerStep", new InactiveUserPartitioner())
            .gridSize(5)
            .step(inactiveJobStep)
            .taskExecutor(taskExecutor())
            .build();
    }

    @Bean
    @StepScope
    public ListItemReader<User> inactiveUserReader(@Value("#{stepExecutionContext[grade]}") String grade, UserRepository userRepository) {
        log.info(Thread.currentThread().getName());
        List<User> inactiveUsers = userRepository.findByUpdatedDateBeforeAndStatusEqualsAndGradeEquals(LocalDateTime.now().minusYears(1), UserStatus.ACTIVE,
            Grade.valueOf(grade));
        return new ListItemReader<>(inactiveUsers);
    }

    @Bean
    public Flow multiFlow(Step inactiveJobStep) {
        Flow flows[] = new Flow[5];
        IntStream.range(0, flows.length).forEach(i -> flows[i] = new FlowBuilder<Flow>("MultiFlow" + i)
            .from(inactiveJobFlow(inactiveJobStep)).end());

        FlowBuilder<Flow> flowBuilder = new FlowBuilder<>("MultiFlowTest");
        return flowBuilder
            .split(taskExecutor())
            .add(flows)
            .build();
    }

    @Bean
    public Flow inactiveJobFlow(Step inactiveJobStep) {
        FlowBuilder<Flow> flowBuilder = new FlowBuilder<>("inactiveJobFlow");
        return flowBuilder.start(new InactiveJobExecutionDecider())
            .on(FlowExecutionStatus.FAILED.getName()).end()
            .on(FlowExecutionStatus.COMPLETED.getName()).to(inactiveJobStep)
            .end();
    }

    @Bean
    public Step inactiveJobStep(StepBuilderFactory stepBuilderFactory, ListItemReader<User> inactiveUserReader, InactiveStepListener inactiveStepListener, TaskExecutor taskExecutor) {
        return stepBuilderFactory.get("inactiveUserStep")
            .<User, User>chunk(CHUNK_SIZE)
            .reader(inactiveUserReader)
            .processor(inactiveUserProcessor())
            .writer(inactiveUserWriter())
            .listener(inactiveStepListener)
            .taskExecutor(taskExecutor)
            .throttleLimit(2)
            .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("Batch_Task");
    }

    // @Bean(destroyMethod = "")
    // @StepScope
    // public JpaPagingItemReader<User> inactiveUserJpaReader() {
    //     JpaPagingItemReader<User> jpaPagingItemReader = new JpaPagingItemReader<>();
    //     jpaPagingItemReader.setQueryString("select u from User as u where u.updateDate < :updatedDate and u.status = :status");
    //     Map<String, Object> map = new HashMap<>();
    //     LocalDateTime now = LocalDateTime.now();
    //     map.put("updatedDate", now.minusYears(1));
    //     map.put("status", UserStatus.ACTIVE);
    //
    //     jpaPagingItemReader.setParameterValues(map);
    //
    //     jpaPagingItemReader.setEntityManagerFactory(entityManagerFactory);
    //     jpaPagingItemReader.setPageSize(CHUNK_SIZE);
    //     return jpaPagingItemReader;
    // }

    // @Bean
    // @StepScope
    // public ListItemReader<User> inactiveUserReader(@Value("#{jobParameters[nowDate]}") Date nowDate, UserRepository userRepository) {
    //     LocalDateTime now = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault());
    //     List<User> inactiveUsers = userRepository.findByUpdatedDateBeforeAndStatusEquals(now.minusYears(1), UserStatus.ACTIVE);
    //     return new ListItemReader<>(inactiveUsers);
    // }

    // @Bean
    // @StepScope
    // public QueueItemReader<User> inactiveUserReader() {
    //     List<User> oldUsers = userRepository.findByUpdatedDateBeforeAndStatusEquals(LocalDateTime.now().minusYears(1), UserStatus.ACTIVE);
    //     return new QueueItemReader<>(oldUsers);
    // }

    public ItemProcessor<User, User> inactiveUserProcessor() {
        return User::setInactive;
        // return new ItemProcessor<User, User>() {
        //     @Override
        //     public User process(User user) throws Exception {
        //         return user.setInactive();
        //     }
        // };
    }

    public JpaItemWriter<User> inactiveUserWriter() {
        JpaItemWriter<User> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
        return jpaItemWriter;
    }

    // public ItemWriter<User> inactiveUserWriter() {
    //     return ((List<? extends User> users) -> userRepository.saveAll(users));
    // }
}

