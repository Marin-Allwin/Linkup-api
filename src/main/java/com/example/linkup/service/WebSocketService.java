package com.example.linkup.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // For sending WebSocket messages


    public void sendFriendRequestNotification(String recipientEmail, String senderEmail) {
        // Construct the notification message
        String notificationMessage = senderEmail + " has sent you a friend request!";

        // Send the message to the recipient's WebSocket topic

        messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/notifications", notificationMessage);
    }
}
