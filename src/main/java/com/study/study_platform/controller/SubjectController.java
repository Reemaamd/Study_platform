package com.study.study_platform.controller;

import com.study.study_platform.dto.SubjectDTO;
import com.study.study_platform.dto.SubjectResponseDTO;
import com.study.study_platform.model.document.Subject;
import com.study.study_platform.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subjects")
public class SubjectController {

    private final SubjectService service;

    public SubjectController(SubjectService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public Subject create(@Valid @RequestBody SubjectDTO dto) {
        return service.createSubject(dto);
    }

    // GET sécurisé (user connecté uniquement)
    @GetMapping
    public List<SubjectResponseDTO> getAll() {
        return service.getSubjectsForCurrentUser();
    }

    // DELETE sécurisé
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.deleteSubject(id);
    }

    // GET by name sécurisé
    @GetMapping("/by-name")
    public List<SubjectResponseDTO> getByName(@RequestParam String name) {
        return service.getByName(name);
    }

    @PutMapping("/{id}")
    public SubjectResponseDTO update(@PathVariable String id, @RequestBody SubjectDTO dto) {
        return service.updateSubject(id, dto);
    }
    @GetMapping("/count")
    public long countMySubjects() {
        return service.countSubjectsForCurrentUser();
    }
    @GetMapping("/admin/count")
    public long countAll() {
        return service.countAllSubjects();
    }
}