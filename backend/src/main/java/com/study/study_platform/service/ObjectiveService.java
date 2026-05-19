package com.study.study_platform.service;

import com.study.study_platform.dto.ObjectiveDTO;
import com.study.study_platform.dto.ObjectiveRequest;
import com.study.study_platform.exception.ObjectiveNotFoundException;
import com.study.study_platform.mapper.ObjectiveMapper;
import com.study.study_platform.model.document.Objective;
import com.study.study_platform.model.document.StudySession;
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

    // ==============================
    // GET USER ID FROM JWT
    // ==============================
    private String getUserId() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    // ==============================
    // VALIDATE SUBJECT BELONGS TO USER
    // ==============================
    private void validateSubjectOwnership(String subjectId, String userId) {
        if (subjectId == null) return;
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        if (!subject.getUserId().equals(userId)) {
            throw new RuntimeException("Subject does not belong to this user");
        }
    }

    // ==============================
    // ENRICH DTO WITH SUBJECT NAME
    // ==============================
    private ObjectiveDTO enrich(Objective obj, Map<String, Subject> subjectMap) {
        ObjectiveDTO dto = objectiveMapper.toDTO(obj);
        Subject subject = obj.getSubjectId() != null
                ? subjectMap.get(obj.getSubjectId())
                : null;
        dto.setSubjectName(subject != null ? subject.getName() : "Unknown");
        return dto;
    }

    // ==============================
    // BUILD SUBJECT MAP
    // ==============================
    private Map<String, Subject> buildSubjectMap(List<Objective> objectives) {
        List<String> subjectIds = objectives.stream()
                .map(Objective::getSubjectId)
                .distinct()
                .toList();
        if (subjectIds.isEmpty()) return new HashMap<>();
        return subjectRepository.findAllById(subjectIds)
                .stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (a, b) -> a));
    }

    // ==============================
    // CREATE
    // ==============================
    public ObjectiveDTO create(ObjectiveRequest request) {

        String userId = getUserId();

        // ✅ VALIDATE SUBJECT OWNERSHIP
        validateSubjectOwnership(request.getSubjectId(), userId);

        Objective obj = objectiveMapper.toEntity(request, userId);

        LocalDate today = LocalDate.now();
        obj.setWeekStartDate(today.with(DayOfWeek.MONDAY));
        obj.setWeekEndDate(today.with(DayOfWeek.SUNDAY));

        objectiveRepository.save(obj);

        return enrich(obj, buildSubjectMap(List.of(obj)));
    }

    // ==============================
    // GET BY ID
    // ==============================
    public ObjectiveDTO getById(String id) {

        String userId = getUserId();

        Objective obj = objectiveRepository.findById(id)
                .orElseThrow(() -> new ObjectiveNotFoundException("Objective not found"));

        if (!obj.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        return enrich(obj, buildSubjectMap(List.of(obj)));
    }

    // ==============================
    // UPDATE PROGRESS
    // ==============================
    public ObjectiveDTO updateProgress(String id, int progress) {

        String userId = getUserId();

        Objective obj = objectiveRepository.findById(id)
                .orElseThrow(() -> new ObjectiveNotFoundException("Objective not found"));

        if (!obj.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        obj.setProgress(progress);

        if (progress >= obj.getWeeklyGoal()) {
            System.out.println("🎯 Objective completed!");
        }

        objectiveRepository.save(obj);

        return enrich(obj, buildSubjectMap(List.of(obj)));
    }

    // ==============================
    // UPDATE
    // ==============================
    public ObjectiveDTO update(String id, ObjectiveRequest request) {

        String userId = getUserId();

        Objective obj = objectiveRepository.findById(id)
                .orElseThrow(() -> new ObjectiveNotFoundException("Objective not found"));

        if (!obj.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // ✅ VALIDATE SUBJECT OWNERSHIP IF CHANGED
        if (request.getSubjectId() != null) {
            validateSubjectOwnership(request.getSubjectId(), userId);
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

        return enrich(obj, buildSubjectMap(List.of(obj)));
    }

    // ==============================
    // DELETE
    // ==============================
    public void delete(String id) {

        String userId = getUserId();

        Objective obj = objectiveRepository.findById(id)
                .orElseThrow(() -> new ObjectiveNotFoundException("Objective not found"));

        if (!obj.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        objectiveRepository.delete(obj);
    }

    // ==============================
    // GET ALL
    // ==============================
    public List<ObjectiveDTO> getAll() {

        String userId = getUserId();

        List<Objective> objectives = objectiveRepository.findByUserId(userId);

        return objectives.stream()
                .map(obj -> enrich(obj, buildSubjectMap(List.of(obj))))
                .collect(Collectors.toList());
    }

    // ==============================
    // GET BY WEEK
    // ==============================
    public List<ObjectiveDTO> getByWeek(LocalDate weekStart, LocalDate weekEnd) {

        String userId = getUserId();

        List<Objective> objectives = objectiveRepository
                .findByUserIdAndWeekStartDateBetween(userId, weekStart, weekEnd);

        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    // ==============================
    // GET BY PRIORITY
    // ==============================
    public List<ObjectiveDTO> getByPriority() {

        List<Objective> objectives = objectiveRepository
                .findByUserIdOrderByPriorityDesc(getUserId());

        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    // ==============================
    // GET ACHIEVED
    // ==============================
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

    // ==============================
    // GET WEEKLY OVERVIEW
    // ==============================
    public List<ObjectiveDTO> getWeeklyOverview(LocalDate date) {

        String userId = getUserId();

        LocalDate start = date.with(DayOfWeek.MONDAY);
        LocalDate end   = date.with(DayOfWeek.SUNDAY);

        List<Objective> objectives = objectiveRepository
                .findByUserIdAndWeekStartDateBetween(userId, start, end);

        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> {
                    ObjectiveDTO dto = enrich(obj, subjectMap);
                    int percentage = obj.getWeeklyGoal() != 0
                            ? (obj.getProgress() * 100) / obj.getWeeklyGoal()
                            : 0;
                    dto.setProgressPercentage(percentage);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ==============================
    // GET HISTORY
    // ==============================
    public List<ObjectiveDTO> getHistory() {

        String userId = getUserId();
        LocalDate today = LocalDate.now();

        List<Objective> objectives = objectiveRepository.findByUserId(userId)
                .stream()
                .filter(o -> o.getWeekEndDate() != null
                        && o.getWeekEndDate().isBefore(today))
                .toList();

        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    // ==============================
    // STATS
    // ==============================
    public long getTotalObjectives() {
        return objectiveRepository.findByUserId(getUserId()).size();
    }

    public long getAchievedCount() {
        return objectiveRepository.findByUserId(getUserId())
                .stream()
                .filter(o -> o.getProgress() >= o.getWeeklyGoal())
                .count();
    }

    public long getPendingCount() {
        return objectiveRepository.findByUserId(getUserId())
                .stream()
                .filter(o -> o.getProgress() < o.getWeeklyGoal())
                .count();
    }

    public List<ObjectiveDTO> getCurrentWeekObjectives(LocalDate start, LocalDate end) {

        List<Objective> objectives = objectiveRepository
                .findByUserIdAndWeekStartDateBetween(getUserId(), start, end);

        Map<String, Subject> subjectMap = buildSubjectMap(objectives);

        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    // ==============================
    // ADMIN
    // ==============================
    public List<ObjectiveDTO> getAllForAdmin() {
        List<Objective> objectives = objectiveRepository.findAll();
        Map<String, Subject> subjectMap = buildSubjectMap(objectives);
        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    public List<ObjectiveDTO> getByUserIdForAdmin(String userId) {
        List<Objective> objectives = objectiveRepository.findByUserId(userId);
        Map<String, Subject> subjectMap = buildSubjectMap(objectives);
        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    public List<ObjectiveDTO> getBySubjectForAdmin(String subjectId) {
        List<Objective> objectives = objectiveRepository.findBySubjectId(subjectId);
        Map<String, Subject> subjectMap = buildSubjectMap(objectives);
        return objectives.stream()
                .map(obj -> enrich(obj, subjectMap))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getGlobalStats() {

        List<Objective> all = objectiveRepository.findAll();

        long total = all.size();
        long achieved = all.stream()
                .filter(o -> o.getProgress() >= o.getWeeklyGoal())
                .count();

        double completionRate = total == 0 ? 0 : (achieved * 100.0) / total;

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
}