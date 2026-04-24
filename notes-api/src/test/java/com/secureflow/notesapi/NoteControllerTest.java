package com.secureflow.notesapi;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link NoteController}.
 *
 * Uses @WebMvcTest which loads ONLY the web layer (the controller) —
 * no database, no full Spring context. Fast (<100ms per test).
 * The NoteRepository is mocked via @MockitoBean.
 */
@WebMvcTest(NoteController.class)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoteRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /notes creates a note and returns 200 OK with saved entity")
    void createNote_returnsOkWithSavedEntity() throws Exception {
        Note incoming = new Note("Buy milk", "2L whole");
        Note saved = new Note("Buy milk", "2L whole");
        saved.setId(42L);

        when(repository.save(any(Note.class))).thenReturn(saved);

        mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incoming)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.title").value("Buy milk"))
                .andExpect(jsonPath("$.content").value("2L whole"));

        verify(repository).save(any(Note.class));
    }

    @Test
    @DisplayName("GET /notes returns all notes as JSON array")
    void listNotes_returnsAll() throws Exception {
        Note a = new Note("First", "aaa");
        a.setId(1L);
        Note b = new Note("Second", "bbb");
        b.setId(2L);

        when(repository.findAll()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/notes"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("First"))
                .andExpect(jsonPath("$[1].title").value("Second"));
    }

    @Test
    @DisplayName("GET /notes returns empty array when no notes exist")
    void listNotes_returnsEmptyArray() throws Exception {
        when(repository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("DELETE /notes/{id} returns 204 No Content when note exists")
    void deleteNote_returnsNoContentWhenExists() throws Exception {
        when(repository.existsById(99L)).thenReturn(true);

        mockMvc.perform(delete("/notes/99"))
                .andExpect(status().isNoContent());

        verify(repository).deleteById(99L);
    }

    @Test
    @DisplayName("DELETE /notes/{id} returns 404 when note does not exist")
    void deleteNote_returns404WhenMissing() throws Exception {
        when(repository.existsById(404L)).thenReturn(false);

        mockMvc.perform(delete("/notes/404"))
                .andExpect(status().isNotFound());
    }
}
