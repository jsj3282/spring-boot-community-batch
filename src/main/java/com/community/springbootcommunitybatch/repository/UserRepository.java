package com.community.springbootcommunitybatch.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.community.springbootcommunitybatch.domain.User;
import com.community.springbootcommunitybatch.domain.enums.Grade;
import com.community.springbootcommunitybatch.domain.enums.UserStatus;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByUpdatedDateBeforeAndStatusEquals(LocalDateTime localDateTime, UserStatus status);

    List<User> findByUpdatedDateBeforeAndStatusEqualsAndGradeEquals(LocalDateTime minusYears, UserStatus active, Grade grade);
}
