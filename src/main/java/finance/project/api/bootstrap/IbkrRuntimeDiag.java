package finance.project.api.bootstrap;


import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IbkrRuntimeDiag {

    public static Map<String, Object> collect() {
        Map<String, Object> m = new LinkedHashMap<>();

        // --- Protobuf runtime (full vs lite), version & jar ---
        Class<?> pbMsg = resolve("com.google.protobuf.Message"); // full & lite
        Class<?> pbV3  = resolve("com.google.protobuf.GeneratedMessageV3"); // full only
        Class<?> pbLite= resolve("com.google.protobuf.GeneratedMessageLite"); // lite only
        m.put("protobuf.flavor",
                pbV3 != null ? "full (protobuf-java)" :
                        pbLite != null ? "lite (protobuf-javalite)" : "unknown");

        m.put("protobuf.version", pkgVersion(pbMsg));
        m.put("protobuf.jar", jarOf(pbMsg));

        // --- IB client jar ---
        Class<?> ibClient = resolve("com.ib.client.EClientSocket");
        m.put("ib.client.jar", jarOf(ibClient));
        m.put("ib.client.packageVersion", pkgVersion(ibClient)); // dépend du manifest

        // --- Une classe protobuf générée par IB : parent utilisé (full vs lite) & jar ---
        Class<?> mdReq = resolve("com.ib.client.protobuf.MarketDataRequestProto$MarketDataRequest");
        m.put("ib.protobuf.classPresent", mdReq != null);
        m.put("ib.protobuf.superclass",
                mdReq != null ? mdReq.getSuperclass().getName() : "n/a");
        m.put("ib.protobuf.jar", jarOf(mdReq));

        // --- Vérif “init statique” (true = OK, false = KO, erreur = message) ---
        m.put("ib.protobuf.staticInit.ok", tryStaticInit(mdReq));

        return m;
    }

    private static Class<?> resolve(String fqn) {
        try { return Class.forName(fqn); } catch (Throwable t) { return null; }
    }

    private static String pkgVersion(Class<?> c) {
        if (c == null) return "n/a";
        Package p = c.getPackage();
        return (p != null && p.getImplementationVersion() != null)
                ? p.getImplementationVersion() : "unknown";
    }

    private static String jarOf(Class<?> c) {
        if (c == null) return "n/a";
        try {
            URL url = c.getProtectionDomain().getCodeSource().getLocation();
            return url != null ? url.toString() : "unknown";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static Object tryStaticInit(Class<?> c) {
        if (c == null) return "class not found";
        try {
            // Beaucoup de messages protobuf ont un builder statique
            c.getMethod("newBuilder").invoke(null);
            return true;
        } catch (Throwable t) {
            return t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage());
        }
    }

    private IbkrRuntimeDiag() {}
}
