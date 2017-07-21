package uk.gov.homeoffice.pontus;

import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ChronicleQueueBuilder;
import net.openhft.chronicle.ExcerptTailer;
import org.graylog2.syslog4j.util.SyslogUtility;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class ChronicleDiscovery {

    public static AtomicBoolean newDataAvailable = new AtomicBoolean(false);
    public static volatile boolean threadRunning = false;


    /**
     * This method looks at the next level directories below the baseDir path and attempts to create Chronicles and add
     * ExcerptTailers for each directory without any filtering mechanism.
     *
     * @param chronicles an outbound array list of chronicles found below the given baseDir
     *                   - note that an array list was chosen rather than just a list
     *                   because this is the most efficient list to traverse using the following method:
     *                   for (int i, ilen = list.size(); i < ilen; i++){}
     * @param tailers    an outbound array list of ExcerptTailers found below the given baseDir
     * @param baseDir    a directory in the filesystem to start searching for Chronicles and ExcerptTailers
     * @throws IOException
     */
    public static void getTailers(ArrayList<Chronicle> chronicles, ArrayList<ExcerptTailer> tailers, Path baseDir) throws IOException {
        getTailers(chronicles, tailers, baseDir, null);
    }

    /**
     * This method looks at the next level directories below the baseDir path and attempts to create Chronicles and add
     * ExcerptTailers for each directory.
     *
     * @param chronicles      an outbound array list of chronicles found below the given baseDir
     *                        - note that an array list was chosen rather than just a list
     *                        because this is the most efficient list to traverse using the following method:
     *                        for (int i, ilen = list.size(); i < ilen; i++){}
     * @param tailers         an outbound array list of ExcerptTailers found below the given baseDir
     * @param baseDir         a directory in the filesystem to start searching for Chronicles and ExcerptTailers
     * @param matchingPattern an optional (if null, all entries are included) regex pattern used to
     *                        include matching entries
     * @throws IOException
     */
    public static void getTailers(ArrayList<Chronicle> chronicles, ArrayList<ExcerptTailer> tailers, Path baseDir, Pattern matchingPattern) throws IOException {

        DirectoryStream<Path> dirStream = Files.newDirectoryStream(baseDir);
        if (matchingPattern != null) {
            for (Path p : dirStream) {
                if (p.toFile().isDirectory()) {
                    Path fileNamePath = p.getFileName();
                    String fileName = fileNamePath.toString();
                    if (matchingPattern.matcher(fileName).matches()) {
                        addChronicleAndExcerptTrailerToLists(p, chronicles, tailers);
                    }
                }
            }
        } else {
            for (Path p : dirStream) {
                if (p.toFile().isDirectory()) {
                    addChronicleAndExcerptTrailerToLists(p, chronicles, tailers);
                }
            }

        }
    }

    private static void addChronicleAndExcerptTrailerToLists(Path fileNamePath, ArrayList<Chronicle> chronicles,
                                                             ArrayList<ExcerptTailer> tailers) throws IOException {
        Chronicle chronicle = ChronicleQueueBuilder.vanilla(fileNamePath.toFile()).build();
        ExcerptTailer tailer = chronicle.createTailer().toStart();
        tailers.add(tailer);
        chronicles.add(chronicle);
    }

    public static void checkForNewFiles(Path baseDir, Pattern matchingPattern) throws IOException {
        long lastTimeChecked = 0L;
        DirectoryStream<Path> dirStream = Files.newDirectoryStream(baseDir);
        if (matchingPattern != null) {
            for (Path p : dirStream) {
                if (p.toFile().isDirectory()) {
                    Path fileNamePath = p.getFileName();
                    String fileName = fileNamePath.toString();
                    if (matchingPattern.matcher(fileName).matches()) {
                        if (p.toFile().lastModified() >= lastTimeChecked) {
                            newDataAvailable.set(true);
                            break;
                        }
                    }
                }
            }
        } else {
            for (Path p : dirStream) {
                if (p.toFile().isDirectory()) {
                    if (p.toFile().lastModified() >= lastTimeChecked) {
                        newDataAvailable.set(true);
                        break;
                    }
                }
            }

        }
        SyslogUtility.sleep(10000);
    }

    public static void tailerDiscoveryThreadWrapper(Path baseDir, Pattern matchingPattern) {

        if (!threadRunning) {
            threadRunning = true;

            Thread thrd = new Thread(() -> {
                try {
                    do {
                        checkForNewFiles(baseDir, matchingPattern);
                    } while (threadRunning);

                } catch (Exception ignored) {
                }
            });

            thrd.setName("chronicle-discovery");
            thrd.start();
        }
    }

    public static boolean checkNewChronicles() {
        return newDataAvailable.get();
    }
}
