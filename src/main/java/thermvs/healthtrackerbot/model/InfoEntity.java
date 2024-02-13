package thermvs.healthtrackerbot.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "info")
public class InfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String userName;

    private String sleepStartTime;

    private String wakeUpTime;

    private String sleepQuality;

    private String daytimeAlertness;
}
