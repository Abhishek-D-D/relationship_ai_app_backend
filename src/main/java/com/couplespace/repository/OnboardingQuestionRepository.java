package com.couplespace.repository;

import com.couplespace.entity.OnboardingQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OnboardingQuestionRepository extends JpaRepository<OnboardingQuestion, UUID> {
}
