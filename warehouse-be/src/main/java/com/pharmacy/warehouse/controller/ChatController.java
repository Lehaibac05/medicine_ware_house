package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.dto.ChatAskRequest;
import com.pharmacy.warehouse.dto.ChatAskResponse;
import com.pharmacy.warehouse.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/ask")
    public ResponseEntity<ChatAskResponse> ask(@RequestBody ChatAskRequest request, Authentication authentication) {
        String message = request == null ? "" : request.getMessage();
        String username = authentication == null ? "" : authentication.getName();
        Set<String> roles = authentication == null
                ? Set.of()
                : authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
        return ResponseEntity.ok(chatService.ask(message, roles, username));
    }
}
