package finance.project.api.controllers;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class CsvMonthlySplitter {

    public static void main(String[] args) {
        String inputFile = "csvData/eurusd/1min/1minCME.csv";
        String outputFolder = "csvData/eurusd/1min/month/";
        splitCsvByMonth(inputFile, outputFolder);
    }
    public static void splitCsvByMonth(String inputFilePath, String outputFolderPath) {
        BufferedReader reader = null;
        BufferedWriter writer = null;

        String currentMonth = "";
        String header = "";

        try {
            reader = Files.newBufferedReader(Paths.get(inputFilePath));

            // Lire l'en-tête
            header = reader.readLine();
            if (header == null) {
                System.out.println("Fichier vide ou pas d'en-tête !");
                return;
            }

            String line;
            long lineCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;

                // Split sur les virgules
                String[] columns = line.split(",");
                if (columns.length < 1) continue; // skip si la ligne est vide ou malformée

                String tsString = columns[0];
                System.out.println("Column[0]: " + tsString);

                long tsNano = Long.parseLong(tsString); // raw number
                long tsMillis = tsNano / 1_000_000L;    // conversion en ms
                Instant instant = Instant.ofEpochMilli(tsMillis); // ms → instant

                ZonedDateTime dateTime = instant.atZone(ZoneId.of("UTC"));
                String yearMonth = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM"));

                // DEBUG : Affichage de la date et de l'année/mois
                System.out.println("Column[0]: " + columns[0]);
                System.out.println("Date UTC : " + dateTime);
                System.out.println("YearMonth : " + yearMonth);

                // DEBUG (optionnel) : Affiche chaque million de lignes
                if (lineCount % 1_000_000 == 0) {
                    System.out.println("Ligne " + lineCount + " - Date : " + dateTime);
                }

                // 6. Si on change de mois, on change de fichier
                if (!yearMonth.equals(currentMonth)) {
                    if (writer != null) {
                        writer.close();  // Ferme l'ancien fichier
                    }

                    // Nouveau fichier pour le mois courant
                    String outputFileName = outputFolderPath + "data_" + yearMonth + ".csv";
                    writer = Files.newBufferedWriter(Paths.get(outputFileName));

                    // Réécrire l'en-tête
                    writer.write(header);
                    writer.newLine();

                    currentMonth = yearMonth;  // Met à jour le mois courant
                    System.out.println("Création du fichier : " + outputFileName);
                }

                // 7. Écrire la ligne dans le fichier courant
                writer.write(line);
                writer.newLine();
            }

            System.out.println("✅ Découpage terminé ! Total lignes : " + lineCount);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
