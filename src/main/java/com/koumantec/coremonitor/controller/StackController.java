package com.koumantec.coremonitor.controller;

import com.koumantec.coremonitor.dto.Dtos;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Reçoit les choix effectués par l'utilisateur dans l'onglet "Composition de la stack".
 * Lie automatiquement le JSON soumis par le frontend à l'objet Dtos.StackCompositionRequest.
 * Conserve en mémoire la dernière soumission pour consultation éventuelle.
 */
@Controller
public class StackController {

    private volatile Dtos.StackCompositionRequest lastSubmission;

    /**
     * Endpoint appelé par le bouton "Créer l'environnement" (frontend: POST "/soumettre").
     * @param request objet contenant tous les choix de l'utilisateur
     * @return la requête soumise en JSON pour confirmation côté client
     */
    @PostMapping("/soumettre")
    @ResponseBody
    public ResponseEntity<Dtos.StackCompositionRequest> submitStackComposition(
            @RequestBody Dtos.StackCompositionRequest request) {
        // Mise à disposition de l'objet dans le controller (ici, conservation en mémoire)
        this.lastSubmission = request;
        // Retourne la requête en JSON pour que le frontend puisse rediriger
        return ResponseEntity.ok(request);
    }

    /**
     * Optionnel: permet de récupérer la dernière soumission reçue (utile pour debugger/valider côté UI).
     * - 204 No Content si aucune soumission n'a été reçue.
     */
    @GetMapping("/soumission")
    @ResponseBody
    public ResponseEntity<Dtos.StackCompositionRequest> getLastSubmission() {
        if (lastSubmission == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(lastSubmission);
    }
}
