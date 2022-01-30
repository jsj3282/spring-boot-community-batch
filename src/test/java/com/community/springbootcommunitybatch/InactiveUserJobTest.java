package com.community.springbootcommunitybatch;

import static com.community.springbootcommunitybatch.domain.enums.UserStatus.*;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.community.springbootcommunitybatch.domain.enums.UserStatus;
import com.community.springbootcommunitybatch.repository.UserRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class InactiveUserJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void inactiveUserJobTest() throws Exception {
        Date nowDate = new Date();
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(
            new JobParametersBuilder().addDate("nowDate", nowDate).toJobParameters());

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        assertEquals(11, userRepository.findAll().size());
        assertEquals(0,
            userRepository.findByUpdatedDateBeforeAndStatusEquals(LocalDateTime.now().minusYears(1), ACTIVE).size());
    }
}
