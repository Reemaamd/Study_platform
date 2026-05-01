package com.study.study_platform.service;

import com.study.study_platform.dto.ObjectiveDTO;
import com.study.study_platform.dto.ObjectiveRequest;
import com.study.study_platform.exception.ObjectiveNotFoundException;
import com.study.study_platform.mapper.ObjectiveMapper;
import com.study.study_platform.model.document.Objective;
import com.study.study_platform.model.document.Subject;
import com.study.study_platform.repository.ObjectiveRepository;
import com.study.study_platform.repository.SubjectRepository;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ObjectiveService {
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final ObjectiveRepository objectiveRepository;
    private final ObjectiveMapper objectiveMapper;

    // GET USER ID FROM JWT
    private String getUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    private ObjectiveDTO enrich(Objective obj, Map<String, Subject> subjectMap) {

        ObjectiveDTO dto = objectiveMapper.toDTO(obj);

        Subject subject = obj.getSubjectId() != null
                ? subjectMap.get(obj.getSubjectId())
                : null;
        dto.setSubjectName(subject != null ? subject.getName() : "Unknown");

        return dto;
    }

    private Map<String, Subject> buildSubjectMap(List<Objective> objectives) {

        List<String> subjectIds = objectives.stream()
                .map(Objective::getSubjectId)
                .distinct()
                .toList();
        if (subjectIds.isEmpty()) {
            return new HashMap<>();
        }
        return subjectRepository.findAllById(subjectIds)
                .stream()
                .collect(Collectors.toMap(
                        Subject::getId,
                        s -> s,
                        (a, b) -> a));
    }

    // CREATE
    public ObjectiveDTO create(ObjectiveRequest request) {
        String userId = getUserId();

        Objective obj = objectiveMapper.toEntity(request, userId);
        objectiveRepository.save(obj);

        Map<String, Subject> subjectMap =
                buildSubjectMap(List.of(obj));

        return enrich(obj, subjectMap);
    }

    // GET BY ID
    public ObjectiveDTO getById(String id) {
        Objective obj = objectiveRepository.findById(id)
                .orElseThrow(() -> new ObjectiveNotFoundException("Objective not found"));
        if (!obj.getUserId().equals(getUserId())) {
            throw new RuntimeException("Unauthorized");
        }
        Map<String, Subject> subjectMap =
                buildSubjectMap(List.of(obj));
        return enrich(obj, subjectMap);
    }

    // UPDATE PROGRESS Objectifs :
    //mise à jour progress
    //calcul percentage
    //check completion
    //future notification placeholder
    public ObjectiveDTO updateProgress(String id, int progress) {

        String userId = getUserId();

        Objective obj = objectiveRepository.findById(id)
                .orElseThrow(() -> new ObjectiveNotFoundException("Objective not found"));

        // 🔒 security check
        if (!obj.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // update progress
        obj.setProgress(progress);

        // calculate percentage (important)
        /*int percentage = 0;
        if (obj.getWeeklyGoal() != 0) {
            percentage = (progress * 100) / obj.getWeeklyGoal();
        }
*/
        // check completion
        if (progress >= obj.getWeeklyGoal()) {
            System.out.println("🎯 Objective completed!");

            // 🔔 future notification hook
            // notificationService.sendObjectiveCompleted(obj);
        }

        objectiveRepository.save(obj);

        // convert + return DTO
        // ObjectiveDTO dto = objectiveMapper.toDTO(obj);
        //dto.setProgressPercentage(percentage); */

        // ✅ build subjectMap
        Map<String, Subject> subjectMap = buildSubjectMap(List.of(obj));

        return enrich(obj, subjectMap);
    }

    // DELETE
    public void delete(String id) {

        String userId = getUserId();

        Objective obj = objectiveRepository.findById(id)
                .orElseThrow(() -> new ObjectiveNotFoundException("Objective not found"));

        if (!obj.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        objectiveRepository.delete(obj);
    }

    // GET BY WEEK (CORRECT VERSION)
    public List<ObjectiveDTO> getByWeek(LocalDate weekStart, LocalDate weekEnd) {

        String userId = getUserId();

        List<Objective> objectives =
                objectiveRepository.findByUserIdAndWeekStartDateBetween(userId, weekStart, weekEnd);

        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    //update() — modifier un objectif
    public ObjectiveDTO update(String id, ObjectiveRequest request) {

        String userId = getUserId();

        Objective obj = objectiveRepository.findById(id)
                .orElseThrow(() -> new ObjectiveNotFoundException("Objective not found"));

        if (!obj.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // ✅ SAFE UPDATE ONLY

        if (request.getSubjectId() != null) {
            obj.setSubjectId(request.getSubjectId());
        }

        if (request.getTitle() != null) {
            obj.setTitle(request.getTitle());
        }

        if (request.getWeeklyGoal() != 0) {
            obj.setWeeklyGoal(request.getWeeklyGoal());
        }

        if (request.getPriority() != 0) {
            obj.setPriority(request.getPriority());
        }

        if (request.getWeekStartDate() != null) {
            obj.setWeekStartDate(request.getWeekStartDate());
        }

        if (request.getWeekEndDate() != null) {
            obj.setWeekEndDate(request.getWeekEndDate());
        }

        objectiveRepository.save(obj);

        Map<String, Subject> subjectMap = buildSubjectMap(List.of(obj));
        return enrich(obj, subjectMap);
    }

    //getByPriority() — triés par priorité décroissante :
    public List<ObjectiveDTO> getByPriority() {

        List<Objective> objectives =
                objectiveRepository.findByUserIdOrderByPriorityDesc(getUserId());

        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    //getAchieved() — objectifs 100% atteints :
    public List<ObjectiveDTO> getAchieved() {

        String userId = getUserId();

        List<Objective> objectives = objectiveRepository.findByUserId(userId)
                .stream()
                .filter(o -> o.getProgress() >= o.getWeeklyGoal())
                .toList();

        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    //TOTAL OBJECTIVES
    public long getTotalObjectives() {
        return objectiveRepository.findByUserId(getUserId()).size();
    }

    //ACHIEVED OBJECTIVES
    public long getAchievedCount() {
        return objectiveRepository.findByUserId(getUserId())
                .stream()
                .filter(o -> o.getProgress() >= o.getWeeklyGoal())
                .count();
    }

    //PENDING OBJECTIVES
    public long getPendingCount() {
        return objectiveRepository.findByUserId(getUserId())
                .stream()
                .filter(o -> o.getProgress() < o.getWeeklyGoal())
                .count();
    }

    //CURRENT WEEK OBJECTIVES
    public List<ObjectiveDTO> getCurrentWeekObjectives(LocalDate start, LocalDate end) {

        List<Objective> objectives =
                objectiveRepository.findByUserIdAndWeekStartDateBetween(getUserId(), start, end);

        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    public List<ObjectiveDTO> getWeeklyOverview(LocalDate date) {

        String userId = getUserId();

        LocalDate start = date.with(DayOfWeek.MONDAY);
        LocalDate end = date.with(DayOfWeek.SUNDAY);
        List<Objective> objectives =
                objectiveRepository.findByUserIdAndWeekStartDateBetween(userId, start, end);

        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> {
                    ObjectiveDTO dto = enrich(obj, subjectMap);

                    int percentage = 0;
                    if (obj.getWeeklyGoal() != 0) {
                        percentage = (obj.getProgress() * 100) / obj.getWeeklyGoal();
                    }

                    dto.setProgressPercentage(percentage);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    //Historique des semaines passées
    public List<ObjectiveDTO> getHistory() {

        String userId = getUserId();
        LocalDate today = LocalDate.now();

        List<Objective> objectives = objectiveRepository.findByUserId(userId)
                .stream()
                .filter(o -> o.getWeekEndDate() != null && o.getWeekEndDate().isBefore(today))
                .toList();

        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    // ADMIN - tous les objectifs de tous les users
    public List<ObjectiveDTO> getAllForAdmin() {
        List<Objective> objectives = objectiveRepository.findAll();
        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    // ADMIN - filtrer par userId
    public List<ObjectiveDTO> getByUserIdForAdmin(String userId) {
        List<Objective> objectives =
                objectiveRepository.findByUserId(userId);
        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    // ADMIN - filtrer par matière
    public List<ObjectiveDTO> getBySubjectForAdmin(String subjectId) {
        List<Objective> objectives =
                objectiveRepository.findBySubjectId(subjectId);
        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    //ADMIN — statistiques globales
    public Map<String, Object> getGlobalStats() {
        List<Objective> all = objectiveRepository.findAll();

        long total = all.size();
        long achieved = all.stream()
                .filter(o -> o.getProgress() >= o.getWeeklyGoal())
                .count();

        double completionRate = total == 0 ? 0 : (achieved * 100.0) / total;

        // matières les plus ciblées
        Map<String, Long> subjectCount = all.stream()
                .collect(Collectors.groupingBy(
                        Objective::getSubjectId,
                        Collectors.counting()
                ));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalObjectives", total);
        stats.put("achievedObjectives", achieved);
        stats.put("completionRate", completionRate);
        stats.put("subjectDistribution", subjectCount);

        return stats;
    }

    public List<ObjectiveDTO> getAll() {

        String userId = getUserId();

        List<Objective> objectives = objectiveRepository.findByUserId(userId);

        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }
}