package thermvs.healthtrackerbot.service;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import thermvs.healthtrackerbot.model.UserEntity;

@Slf4j
@Service
public class MessageService {
    private final QueueMessagingTemplate template;

    @Value("${message.queue.outgoing}")
    private String messageQueueName;

    public MessageService(QueueMessagingTemplate template) {
        this.template = template;
    }

    public void sendMessage(UserEntity user) {
        template.convertAndSend(messageQueueName, user);
        log.info("Message was sent: {} ", user);
    }
}
