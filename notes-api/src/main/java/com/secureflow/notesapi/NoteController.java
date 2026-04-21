package com.secureflow.notesapi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public NoteController(NoteRepository repository) {
        this.repository = repository;
    }

    // POST /notes  — create a new note
    @PostMapping
    public ResponseEntity<Note> create(@RequestBody Note note) {
        Note saved = repository.save(note);
        return ResponseEntity.ok(saved);
    }

    // GET /notes  — list all notes
    @GetMapping
    public List<Note> list() {
        return repository.findAll();
    }

    // DELETE /notes/{id}  — delete a note by id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // GET /notes/search?title=...  — intentional SQL injection demo.
    // DO NOT USE IN PRODUCTION. This endpoint exists to demonstrate that
    // Semgrep catches string-concatenated SQL queries (OWASP Top 10: A03 Injection).
    @GetMapping("/search")
    @SuppressWarnings("unchecked")
    public List<Note> searchByTitle(@RequestParam String title) {
        // VULNERABLE: title is concatenated directly into SQL with no parameterization.
        // An attacker sending title=foo' OR '1'='1 would bypass the filter entirely.
        String sql = "SELECT * FROM notes WHERE title = '" + title + "'";
        return entityManager.createNativeQuery(sql, Note.class).getResultList();
    }
}