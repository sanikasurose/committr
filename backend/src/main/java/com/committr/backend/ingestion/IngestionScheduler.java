package com.committr.backend.ingestion;

import com.committr.backend.repository.RepositoryEntity;
import com.committr.backend.repository.RepositoryRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

    private final IngestionService ingestionService;
    private final RepositoryRepository repositoryRepository;

    public IngestionScheduler(IngestionService ingestionService, RepositoryRepository repositoryRepository) {
        this.ingestionService = ingestionService;
        this.repositoryRepository = repositoryRepository;
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void runScheduledIngestion() {
        List<RepositoryEntity> repos = repositoryRepository.findAllByDeletedAtIsNull();
        log.info("Scheduled ingestion starting — {} repositories to process", repos.size());
        for (RepositoryEntity repo : repos) {
            try {
                ingestionService.ingest(repo.getId());
            } catch (Exception ex) {
                log.error("Ingestion failed for repo {} ({}): {}", repo.getId(), repo.getFullName(), ex.getMessage());
            }
        }
        log.info("Scheduled ingestion complete");
    }
}
