import org.example.services.EventCoverPhotoService;
import java.util.*;

public class EventCoverServiceProbe {
    public static void main(String[] args) throws Exception {
        EventCoverPhotoService svc = new EventCoverPhotoService();
        System.out.println("boxing=" + svc.resolveCoverImageAsync("event", "Boxing", "Seance de Musculation - Test Fidelite").get().isPresent());
        System.out.println("cardio=" + svc.resolveCoverImageAsync("event", "Cardio", "Seance de Musculation - Test Fidelite").get().isPresent());
        System.out.println("circuit=" + svc.resolveCoverImageAsync("event", "CircuitttttttttttttttTtt", "Course en circuit urbainnnnnnnnnnnn").get().isPresent());
        System.out.println("football=" + svc.resolveCoverImageAsync("football", "Football", "Match de football").get().isPresent());
    }
}
