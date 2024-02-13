package thermvs.healthtrackerbot.repository;

import org.springframework.data.repository.CrudRepository;
import thermvs.healthtrackerbot.model.InfoEntity;
import java.util.List;

public interface InfoRepository extends CrudRepository<InfoEntity, Long> {
    List<InfoEntity> findAllByUserName(String userName);
}
