package finance.project.api.repositories;

import finance.project.api.entities.quant.ApiJobEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiJobRepository extends JpaRepository<ApiJobEntity, Long> {
    Optional<ApiJobEntity> findByJobId(String jobId);

    @Query("""
            select j
            from ApiJobEntity j
            where j.jobType in :jobTypes
              and j.status = :status
            order by j.finishedAt desc, j.jobId desc
            """)
    List<ApiJobEntity> findByJobTypeInAndStatusOrderByFinishedAtDescJobIdDesc(
            @Param("jobTypes") Collection<String> jobTypes,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
            select j
            from ApiJobEntity j
            where j.jobType in :jobTypes
              and j.status = :status
              and (
                    j.finishedAt < :cursorFinishedAt
                    or (j.finishedAt = :cursorFinishedAt and j.jobId < :cursorJobId)
                    or j.finishedAt is null
              )
            order by j.finishedAt desc, j.jobId desc
            """)
    List<ApiJobEntity> findAfterCursorWithNonNullFinishedAt(
            @Param("jobTypes") Collection<String> jobTypes,
            @Param("status") String status,
            @Param("cursorFinishedAt") Instant cursorFinishedAt,
            @Param("cursorJobId") String cursorJobId,
            Pageable pageable
    );

    @Query("""
            select j
            from ApiJobEntity j
            where j.jobType in :jobTypes
              and j.status = :status
              and j.finishedAt is null
              and j.jobId < :cursorJobId
            order by j.finishedAt desc, j.jobId desc
            """)
    List<ApiJobEntity> findAfterCursorWithNullFinishedAt(
            @Param("jobTypes") Collection<String> jobTypes,
            @Param("status") String status,
            @Param("cursorJobId") String cursorJobId,
            Pageable pageable
    );
}
