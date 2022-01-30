package com.community.springbootcommunitybatch.jobs.inactive.listener;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InactiveStepListener {

    @BeforeJob
    public void beforeStep(StepExecution stepExecution) {
        log.info("Before Step");
    }

    @AfterJob
    public void afterStep(StepExecution stepExecution) {
        log.info("After Step");
    }
}
