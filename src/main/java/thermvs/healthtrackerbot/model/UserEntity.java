package thermvs.healthtrackerbot.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private long id;

    private String firstName;

    private String lastName;

    private String userName;
}
