package finance.project.api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "killzones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Killzone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String timezone;
    private int year;
    private LocalDate startDate;
    private LocalDate endDate;
}
