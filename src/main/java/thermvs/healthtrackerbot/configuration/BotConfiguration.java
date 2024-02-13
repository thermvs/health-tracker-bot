package thermvs.healthtrackerbot.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Data
public class BotConfiguration {
    @Value("${bot.name}")
    String botName;

    @Value("${bot.token}")
    String token;
}
