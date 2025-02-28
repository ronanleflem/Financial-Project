package finance.project.api.services;

import finance.project.api.entities.Killzone;
import finance.project.api.repositories.KillzoneRepository;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.List;

@Service
public class KillzoneService {

    private final KillzoneRepository killzoneRepository;

    public KillzoneService(KillzoneRepository killzoneRepository) {
        this.killzoneRepository = killzoneRepository;
    }

    public List<Killzone> getKillzonesForYear(int year) {
        return killzoneRepository.findByYear(year);
    }

    public boolean isWithinKillzone(ZonedDateTime timestamp, String sessionName) {
        List<Killzone> killzones = killzoneRepository.findByYear(timestamp.getYear());

        for (Killzone killzone : killzones) {
            if (killzone.getSessionName().equalsIgnoreCase(sessionName)) {
                ZonedDateTime startUTC = convertToUTC(killzone.getStartTime(), killzone.getTimezone(), timestamp);
                ZonedDateTime endUTC = convertToUTC(killzone.getEndTime(), killzone.getTimezone(), timestamp);

                if (!timestamp.isBefore(startUTC) && !timestamp.isAfter(endUTC)) {
                    return true; // Timestamp est dans la killzone
                }
            }
        }
        return false;
    }

    private ZonedDateTime convertToUTC(LocalTime localTime, String timezone, ZonedDateTime referenceDate) {
        ZoneId zoneId = ZoneId.of(timezone);
        LocalDate localDate = referenceDate.toLocalDate();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDate, localTime, zoneId);
        return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
    }
}
