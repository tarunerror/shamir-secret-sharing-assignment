import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import com.google.gson.*;

public class ShamirSecretRecovery {

    public static void main(String[] args) throws Exception {
        // Load input from local JSON files
        String jsonData1 = Files.readString(Paths.get("test1.json"));
        String jsonData2 = Files.readString(Paths.get("test2.json"));

        // Recover secrets from both shares
        BigInteger recovered1 = recoverSecretFromJson(jsonData1);
        BigInteger recovered2 = recoverSecretFromJson(jsonData2);

        System.out.println("Secret 1: " + recovered1);
        System.out.println("Secret 2: " + recovered2);
    }

    private static BigInteger recoverSecretFromJson(String jsonContent) {
        JsonObject json = JsonParser.parseString(jsonContent).getAsJsonObject();
        JsonObject meta = json.getAsJsonObject("keys");

        int totalParts = meta.get("n").getAsInt();
        int threshold = meta.get("k").getAsInt(); // min parts needed to recover

        List<SharePoint> dataPoints = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            if (key.equals("keys")) continue;

            int xCoord = Integer.parseInt(key);
            JsonObject obj = entry.getValue().getAsJsonObject();
            int base = Integer.parseInt(obj.get("base").getAsString());
            String encoded = obj.get("value").getAsString();

            // Decode the Y value using given base
            BigInteger yVal = decode(encoded, base);
            dataPoints.add(new SharePoint(BigInteger.valueOf(xCoord), yVal));
        }

        if (dataPoints.size() < threshold) {
            throw new IllegalStateException("Not enough points to reconstruct secret.");
        }

        // Select first k points (could randomize for more realism)
        List<SharePoint> selected = dataPoints.subList(0, threshold);
        return interpolateAtZero(selected);
    }

    private static BigInteger decode(String value, int base) {
        // Lowercase needed for bases > 10 (a-f)
        return new BigInteger(value.toLowerCase(Locale.ROOT), base);
    }

    private static BigInteger interpolateAtZero(List<SharePoint> points) {
        BigInteger sum = BigInteger.ZERO;

        for (int i = 0; i < points.size(); i++) {
            BigInteger xi = points.get(i).x;
            BigInteger yi = points.get(i).y;

            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;

            for (int j = 0; j < points.size(); j++) {
                if (i == j) continue;

                BigInteger xj = points.get(j).x;

                num = num.multiply(xj.negate());          // 0 - xj = -xj
                den = den.multiply(xi.subtract(xj));      // xi - xj
            }

            // Using exact division since it's guaranteed
            BigInteger term = yi.multiply(num).divide(den);
            sum = sum.add(term);
        }

        return sum;
    }

    // Just a container for x and y values
    static class SharePoint {
        BigInteger x, y;

        SharePoint(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }
}
