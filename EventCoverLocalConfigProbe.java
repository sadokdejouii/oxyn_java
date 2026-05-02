import org.example.services.EventCoverPhotoService;

public class EventCoverLocalConfigProbe {
    public static void main(String[] args) throws Exception {
        EventCoverPhotoService svc = new EventCoverPhotoService();
        System.out.println(svc.resolveCoverImageAsync("event", "Boxing", "Seance de Musculation - Test Fidelite").get().isPresent());
    }
}
