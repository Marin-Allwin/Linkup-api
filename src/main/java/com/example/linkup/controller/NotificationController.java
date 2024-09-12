package com.example.linkup.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class NotificationController {
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/notification")
    public void sendNotification(String message) {
        // Handle the message received from the frontend and send it to a topic
        messagingTemplate.convertAndSend("/public/notifications", message);
    }
}

