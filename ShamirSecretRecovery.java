import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import com.google.gson.*;

public class ShamirSecretRecovery {

    public static void main(String[] args) throws Exception {
        // Load JSON from files (Assuming files test1.json and test2.json)
        String json1 = new String(Files.readAllBytes(Paths.get("test1.json")));
        String json2 = new String(Files.readAllBytes(Paths.get("test2.json")));

        BigInteger secret1 = findSecretFromJson(json1);
        BigInteger secret2 = findSecretFromJson(json2);

        // Print both secrets simultaneously
        System.out.println("Secret 1: " + secret1);
        System.out.println("Secret 2: " + secret2);
    }

    private static BigInteger findSecretFromJson(String jsonStr) {
        JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();

        JsonObject keys = root.getAsJsonObject("keys");
        int n = keys.get("n").getAsInt();
        int k = keys.get("k").getAsInt();  // minimal points required = degree + 1

        // Extract and decode points
        // Each key except "keys" is an x value as string, with base and value field
        List<Point> points = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey();
            if (key.equals("keys")) continue;
            int x = Integer.parseInt(key);

            JsonObject valObj = entry.getValue().getAsJsonObject();
            int base = Integer.parseInt(valObj.get("base").getAsString());
            String valStr = valObj.get("value").getAsString();

            BigInteger y = decodeValue(valStr, base);
            points.add(new Point(BigInteger.valueOf(x), y));
        }

        // We only need k points
        if (points.size() < k) {
            throw new RuntimeException("Insufficient points for interpolation");
        }

        // Pick first k points (could use any k points since polynomial is unique)
        List<Point> selectedPoints = points.subList(0, k);

        // Compute f(0) by Lagrange interpolation
        return lagrangeInterpolationAtZero(selectedPoints);
    }

    // Decode given value string in given base to BigInteger
    // Base can be up to 16 (hex) or even 15 (alphanumeric) according to problem example,
    // so we handle digits and alphabets for bases >10.
    private static BigInteger decodeValue(String valStr, int base) {
        // The value string is a number in the given base
        // Java's BigInteger has constructor: BigInteger(String val, int radix)
        // But if bases > 10 and characters are alphabets, they have to be lowercase hex (a-f) for base 15 or 16
        valStr = valStr.toLowerCase(Locale.ROOT);
        return new BigInteger(valStr, base);
    }

    // Calculate f(0) using Lagrange Interpolation with BigInteger arithmetic
    private static BigInteger lagrangeInterpolationAtZero(List<Point> points) {
        BigInteger result = BigInteger.ZERO;

        int k = points.size();

        for (int i = 0; i < k; i++) {
            BigInteger xi = points.get(i).x;
            BigInteger yi = points.get(i).y;

            BigInteger numerator = BigInteger.ONE;    // product over (0 - xj)
            BigInteger denominator = BigInteger.ONE;  // product over (xi - xj)

            for (int j = 0; j < k; j++) {
                if (j == i) continue;
                BigInteger xj = points.get(j).x;

                numerator = numerator.multiply(xj.negate()); // 0 - xj = -xj
                denominator = denominator.multiply(xi.subtract(xj));
            }

            // fraction = numerator/denominator
            // multiply by yi
            // Since no modulus, do exact division with BigInteger
            // denominator divides numerator at the end because polynomial interpolation guarantees it

            BigInteger term = yi.multiply(numerator).divide(denominator);

            result = result.add(term);
        }
        return result;
    }

    static class Point {
        BigInteger x, y;

        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }
}
