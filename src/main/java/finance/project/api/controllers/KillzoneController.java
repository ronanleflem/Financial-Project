package finance.project.api.controllers;

import finance.project.api.entities.Killzone;
import finance.project.api.services.KillzoneService;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/killzones")
public class KillzoneController {

    private final KillzoneService killzoneService;

    public KillzoneController(KillzoneService killzoneService) {
        this.killzoneService = killzoneService;
    }

    @GetMapping("/{year}")
    public List<Killzone> getKillzones(@PathVariable int year) {
        return killzoneService.getKillzonesForYear(year);
    }

    @GetMapping("/check")
    public boolean isWithinKillzone(@RequestParam String timestamp, @RequestParam String session) {
        ZonedDateTime dateTime = ZonedDateTime.parse(timestamp);
        return killzoneService.isWithinKillzone(dateTime, session);
    }
}
