import org.example.services.WeatherService;
import java.util.Arrays;
import java.util.List;

public class WeatherAPITester {
    public static void main(String[] args) throws Exception {
        WeatherService service = new WeatherService();
        List<String> cities = Arrays.asList("Singapore", "Mumbai", "Bangkok", "Miami", "Jakarta", "Tunis");
        
        System.out.println("========== TESTING WEATHER API ==========\n");
        
        for (String city : cities) {
            try {
                WeatherService.WeatherResult result = service.getWeather(city);
                System.out.println("📍 " + city);
                System.out.println("   Icon: " + result.iconCode + " | Temp: " + result.temperature + "°C");
                System.out.println("   Description: " + result.description);
                System.out.println("   Rain: " + result.rainVolume + " mm | Snow: " + result.snowVolume + " mm");
                System.out.println("   isRainy(): " + result.isRainy());
                System.out.println("   [DEBUG] " + result.getDebugInfo());
                System.out.println();
            } catch (Exception e) {
                System.out.println("❌ " + city + ": " + e.getMessage() + "\n");
            }
        }
    }
}
