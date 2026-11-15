// Speedrun – führt alle Beispiele des Begleit-Repositorys „openHAB & Android-Entwicklung“
// nacheinander gegen ein echtes openHAB aus und baut die Apps. Nicht Teil des
// Begleit-Repositorys. Braucht nur ein JDK ab 21 (startet direkt aus dem Quelltext).
//
//   cd openHAB-Android-Entwicklung-Codes        # Begleit-Repository mit .env
//   ../speedrun/speedrun.sh                     # alles (lädt .env)
//   ../speedrun/speedrun.sh kapitel14           # nur Schritte mit diesem Anfang
//   ../speedrun/speedrun.sh --liste             # nur anzeigen, was laufen würde
//   ../speedrun/speedrun.sh --ohne-apps         # ohne Android SDK: Apps nicht bauen
//   ../speedrun/speedrun.sh --ohne-gradle       # build/ ist schon gebaut
//
// Jede Ausgabe landet in ergebnisse/<schritt>.txt neben diesem Programm.
// OK      = Code 0 (bzw. wie vorgesehen nach Zeitablauf beendet), keine Ausnahme
// WARNUNG = beendet, aber die Ausgabe enthält "error" – bitte ansehen
// FEHLER  = Ausnahme, Rückgabecode ungleich 0 oder Zeitüberschreitung
//
// Achtung: Die Beispiele schalten Items, legen Items, Things, Regeln und Sitemaps an
// und löschen sie wieder. Nur gegen die Übungsinstanz ausführen!
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Speedrun {

    record Schritt(String name, List<String> befehl, String ordner, int timeout,
                   boolean gewollt, Map<String, String> env, List<String[]> ausloesen) {}

    record Ergebnis(int code, String out, boolean abgelaufen) {}

    static final boolean WINDOWS = System.getProperty("os.name").startsWith("Windows");
    static final Path ROOT = Path.of(System.getenv().getOrDefault("REPO", "."))
            .toAbsolutePath().normalize();
    static final Path LOG = Path.of(System.getProperty("speedrun.log", "ergebnisse"))
            .toAbsolutePath();
    static final String URL = System.getenv().getOrDefault("OPENHAB_URL",
            "http://127.0.0.1:8080").replaceAll("/$", "");
    static final String TOKEN = System.getenv("OPENHAB_TOKEN");
    static final List<Schritt> SCHRITTE = new ArrayList<>();
    static final List<String[]> LICHT = List.of(new String[] {"Wohnzimmer_Licht", "ON"},
            new String[] {"Wohnzimmer_Licht", "OFF"});
    static final HttpClient HTTP = HttpClient.newHttpClient();

    static List<String> gradle(String ordner, String... aufgaben) {
        List<String> b = new ArrayList<>();
        if (WINDOWS) {
            b.addAll(List.of("cmd", "/c", ordner + "\\gradlew.bat"));
        } else {
            b.add(ordner + "/gradlew");
        }
        b.add("-q");
        b.addAll(List.of(aufgaben));
        return b;
    }

    // ein Beispiel: kapitel10_erste_schritte/B01_Verbinden -> kapitel10_erste_schritte.B01_VerbindenKt
    static List<String> beispiel(String datei, String... args) {
        String cp = "build/classes/kotlin/main" + File.pathSeparator + "build/lib/*";
        List<String> b = new ArrayList<>(List.of("java", "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8", "-cp", cp, datei.replace('/', '.') + "Kt"));
        b.addAll(List.of(args));
        return b;
    }

    static void s(String name, List<String> befehl, String ordner, int timeout,
                  boolean gewollt, Map<String, String> env, List<String[]> ausloesen) {
        SCHRITTE.add(new Schritt(name, befehl, ordner, timeout, gewollt, env, ausloesen));
    }

    static void schritte(boolean mitGradle, boolean mitApps) {
        if (mitGradle) {
            s("kapitel08/gradle_bereit", gradle(".", "bereit"), ".", 900, false, Map.of(),
                    List.of());
        }
        Set<String> gewollt = Set.of("B05_HttpRoh", "B05_FehlerBehandeln", "B01_ItemsCrud",
                "B01_Persistenzdienste");
        Map<String, List<String>> kapitel = new LinkedHashMap<>();
        kapitel.put("kapitel08_kotlin", List.of("B01_WerteUndTypen", "B02_Kontrollfluss",
                "B03_Funktionen", "B04_Klassen", "B05_FehlerUndJson", "B06_Collections",
                "B07_GenerikaUndSealed"));
        kapitel.put("kapitel09_coroutines", List.of("B01_Coroutines", "B02_Abbrechen",
                "B03_Flows", "B04_Dateien", "B05_HttpRoh", "B06_ProzessUndSignale"));
        kapitel.put("kapitel10_erste_schritte", List.of("B01_Verbinden", "B02_ItemsLesen",
                "B03_CommandSenden", "B04_StateMelden", "B05_FehlerBehandeln",
                "B06_Parallel", "B07_OptionaleParameter"));
        kapitel.put("kapitel11_items", List.of("B01_ItemsCrud", "B02_VieleItems",
                "B03_Metadaten", "B04_Semantik", "B05_GruppenUndVerlauf"));
        kapitel.put("kapitel12_things", List.of("B01_Addons", "B02_Bauplaene",
                "B03_DiscoveryInbox", "B04_ThingsVerwalten", "B05_Links", "B06_Actions"));
        kapitel.put("kapitel13_regeln", List.of("B01_RegelnLesen", "B02_RegelAnlegen",
                "B03_RegelSteuern", "B04_ZeitplanUndSkript", "B05_Modultypen"));
        kapitel.put("kapitel14_daten", List.of("B01_Persistenzdienste", "B02_Testdaten",
                "B03_Verlauf", "B04_DatenLoeschen", "B05_Transformationen", "B02_Testdaten",
                "B06_CsvExport"));
        kapitel.put("kapitel15_oberflaechen", List.of("B01_SitemapLesen",
                "B02_SitemapAnlegen", "B03_SitemapEreignisse", "B04_Sprache", "B05_System"));
        kapitel.put("kapitel16_ereignisse", List.of("B01_SseRoh", "B02_Ereigniskette",
                "B03_Ereignisarten", "B04_StateTracker"));
        kapitel.forEach((k, dateien) -> {
            Set<String> gesehen = new HashSet<>();
            for (String d : dateien) {
                String name = k.substring(0, 9) + "/" + d + (gesehen.add(d) ? "" : "_erneut");
                s(name, beispiel(k + "/" + d), ".", d.equals("B05_Transformationen") ? 180 : 60,
                        gewollt.contains(d),
                        d.equals("B06_ProzessUndSignale") ? Map.of("DEMO_SEKUNDEN", "3")
                                                          : Map.of(), List.of());
            }
        });
        s("kapitel16/B05_RobusterListener", beispiel("kapitel16_ereignisse/B05_RobusterListener",
                "--trennen", "--dauer=10"), ".", 30, false, Map.of(), LICHT);
        s("kapitel16/B06_EreignisseVerteilen",
                beispiel("kapitel16_ereignisse/B06_EreignisseVerteilen"), ".", 60, false,
                Map.of(), List.of());
        if (mitGradle) {
            s("kapitel19/gradle_test", gradle(".", "test", "--rerun-tasks"), ".", 900, false,
                    Map.of(), List.of());
        }
        s("kapitel20/B01_MeldungenEinrichten",
                beispiel("kapitel20_projekte/B01_MeldungenEinrichten"), ".", 60, false,
                Map.of(), List.of());
        if (mitApps) {
            s("apps/assembleDebug", gradle(".", "assembleDebug"), "apps", 1800, false,
                    Map.of(), List.of());
            s("apps/testDebugUnitTest", gradle(".", "testDebugUnitTest"), "apps", 900, false,
                    Map.of(), List.of());
        }
    }

    static String rest(String methode, String pfad, String daten) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(URL + "/rest" + pfad))
                .header("Authorization", "Bearer " + TOKEN)
                .header("Content-Type", "text/plain")
                .method(methode, daten == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(daten));
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    static void ausloesen(List<String[]> befehle) {
        if (befehle.isEmpty()) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(2500);
                for (String[] b : befehle) {
                    rest("POST", "/items/" + b[0], b[1]);
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                System.out.println("  Auslösen: " + e);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    static Ergebnis lauf(Schritt s, Path aus) throws Exception {
        var pb = new ProcessBuilder(s.befehl()).directory(ROOT.resolve(s.ordner()).toFile())
                .redirectErrorStream(true).redirectOutput(aus.toFile());
        pb.environment().putAll(s.env());
        Process p = pb.start();
        ausloesen(s.ausloesen());
        boolean fertig = p.waitFor(s.timeout(), TimeUnit.SECONDS);
        if (!fertig) {
            p.descendants().forEach(ProcessHandle::destroy);
            p.destroy();
            if (!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroyForcibly();
            }
        }
        String out = new String(Files.readAllBytes(aus), StandardCharsets.UTF_8);
        return new Ergebnis(fertig ? p.exitValue() : -1, out, !fertig);
    }

    static String bewerte(Ergebnis e, Schritt s) {
        boolean stacktrace = Pattern.compile("\\n\\s+at [\\w.$]+\\(").matcher(e.out()).find();
        if (stacktrace && !s.gewollt()) {
            return "FEHLER";
        }
        if (e.abgelaufen() || e.code() != 0) {
            return "FEHLER";
        }
        if (e.out().toLowerCase().contains("error") && !s.gewollt()) {
            return "WARNUNG";
        }
        return "OK";
    }

    public static void main(String[] args) throws Exception {
        List<String> argliste = List.of(args);
        schritte(!argliste.contains("--ohne-gradle"), !argliste.contains("--ohne-apps"));
        if (argliste.contains("--liste")) {
            for (Schritt s : SCHRITTE) {
                System.out.printf("%-38s %-6s %s%n", s.name(), s.ordner(),
                        String.join(" ", s.befehl()));
            }
            return;
        }
        List<String> filter = argliste.stream().filter(a -> !a.startsWith("-")).toList();
        if (!Files.isDirectory(ROOT.resolve("kapitel10_erste_schritte"))) {
            System.out.println(ROOT + " ist nicht das Begleit-Repository.");
            System.exit(2);
        }
        if (TOKEN == null) {
            System.out.println("OPENHAB_TOKEN fehlt – .env im Begleit-Repository anlegen.");
            System.exit(2);
        }
        String version;
        try {
            var m = Pattern.compile(
                    "\"runtimeInfo\"\\s*:\\s*\\{[^}]*\"version\"\\s*:\\s*\"([^\"]+)\"")
                    .matcher(rest("GET", "/", null));
            version = m.find() ? m.group(1) : "?";
        } catch (Exception e) {
            System.out.println("openHAB nicht erreichbar: " + e.getMessage());
            System.exit(2);
            return;
        }
        System.out.println("openHAB " + version + " unter " + URL + "\n");
        Files.createDirectories(LOG);
        Map<String, List<String>> nachStatus = new LinkedHashMap<>();
        for (String st : List.of("OK", "WARNUNG", "FEHLER")) {
            nachStatus.put(st, new ArrayList<>());
        }
        for (Schritt s : SCHRITTE) {
            if (!filter.isEmpty() && filter.stream().noneMatch(s.name()::startsWith)) {
                continue;
            }
            long t0 = System.currentTimeMillis();
            Path aus = LOG.resolve(s.name().replace("/", "__") + ".txt");
            Ergebnis e = lauf(s, aus);
            String status = bewerte(e, s);
            System.out.printf("%-38s %-8s %6.1f s%n", s.name(), status,
                    (System.currentTimeMillis() - t0) / 1000.0);
            nachStatus.get(status).add(s.name());
        }
        System.out.println();
        nachStatus.forEach((st, namen) -> System.out.printf("%-8s %3d%s%n", st, namen.size(),
                namen.isEmpty() || st.equals("OK") ? "" : ":  " + String.join(", ", namen)));
        System.out.println("\nAusgaben: " + LOG);
        System.exit(nachStatus.get("FEHLER").isEmpty() ? 0 : 1);
    }
}
