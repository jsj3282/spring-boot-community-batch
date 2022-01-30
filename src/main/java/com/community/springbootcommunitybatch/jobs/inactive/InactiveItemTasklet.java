package com.community.springbootcommunitybatch.jobs.inactive;

import static java.time.ZoneId.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.community.springbootcommunitybatch.domain.User;
import com.community.springbootcommunitybatch.domain.enums.UserStatus;
import com.community.springbootcommunitybatch.repository.UserRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class InactiveItemTasklet implements Tasklet {

    private UserRepository userRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // reader
        Date nowDate = (Date)chunkContext.getStepContext().getJobParameters().get("nowDate");
        LocalDateTime now = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault());
        List<User> inactiveUsers = userRepository.findByUpdatedDateBeforeAndStatusEquals(now.minusYears(1), UserStatus.ACTIVE);
        // processor
        inactiveUsers = inactiveUsers.stream().map(User::setInactive).collect(Collectors.toList());
        // writer
        userRepository.saveAll(inactiveUsers);

        return RepeatStatus.FINISHED;
    }
}
