package thermvs.healthtrackerbot.repository;

import org.springframework.data.repository.CrudRepository;
import thermvs.healthtrackerbot.model.UserEntity;
import java.util.List;

public interface UserRepository extends CrudRepository<UserEntity, Long> {
    List<UserEntity> findAllByUserName(String userName);
}
