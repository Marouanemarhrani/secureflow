package com.secureflow.notesapi;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteRepository repository;

    // Constructor injection — Spring passes the repository in automatically
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
}