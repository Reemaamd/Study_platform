package com.study.study_platform.repository;

import com.study.study_platform.model.document.Objective;
import com.study.study_platform.model.document.Role;
import com.study.study_platform.model.document.Subject;
import com.study.study_platform.model.document.Utilisateur;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ObjectiveRepository extends MongoRepository<Objective, String> {

    List<Objective> findByUserId(String userId);
    List<Objective> findBySubjectId(String subjectId);
    List<Objective> findByUserIdAndSubjectId(String userId, String subjectId);
    List<Objective> findByUserIdAndProgressGreaterThanEqual(String userId, int goal);
    List<Objective> findByUserIdOrderByPriorityDesc(String userId);
    List<Objective> findByUserIdAndWeekStartDateBetween(String userId, LocalDate start, LocalDate end);;
}