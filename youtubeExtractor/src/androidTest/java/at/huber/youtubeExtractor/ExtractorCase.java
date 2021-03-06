package at.huber.youtubeExtractor;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.test.InstrumentationTestCase;
import android.util.Log;
import android.util.SparseArray;

public class ExtractorCase extends InstrumentationTestCase {

    private static final String EXTRACTOR_TEST_TAG = "Extractor Test";

    private String testUrl;

    public void testUsualVideo() throws Throwable {
        VideoMeta expMeta = new VideoMeta("YE7VzlLtp-4", "Big Buck Bunny", "Blender",
                "UCSMOQeBJ2RAnuFungnQOxLg", 597, 0, false);
        extractorTest("http://youtube.com/watch?v=YE7VzlLtp-4", expMeta);
        extractorTestDashManifest("http://youtube.com/watch?v=YE7VzlLtp-4");
    }


    public void testEncipheredVideo() throws Throwable {
        VideoMeta expMeta = new VideoMeta("e8X3ACToii0", "Rise Against - Savior", "RiseAgainstVEVO",
                "UChMKB2AHNpeuWhalpRYhUaw", 243, 0, false);
        extractorTest("https://www.youtube.com/watch?v=e8X3ACToii0", expMeta);
    }

    public void testAgeRestrictVideo() throws Throwable {
        VideoMeta expMeta = new VideoMeta("61Ev-YvBw2c", "Test video for age-restriction",
                "jpdemoA", "UC95NqtFsDZKlmzOJmZi_g6Q", 14, 0, false);
        extractorTest("http://www.youtube.com/watch?v=61Ev-YvBw2c", expMeta);
        extractorTestDashManifest("http://www.youtube.com/watch?v=61Ev-YvBw2c");
    }

    public void testLiveStream() throws Throwable {
        VideoMeta expMeta = new VideoMeta("ddFvjfvPnqk", "NASA Live Stream - Earth From Space (Full Screen) | ISS LIVE FEED - Debunk Flat Earth",
                "Space Videos", "UCakgsb0w7QB0VHdnCc-OVEA", 0, 0, true);
        extractorTest("http://www.youtube.com/watch?v=ddFvjfvPnqk", expMeta);
    }


    private void extractorTestDashManifest(final String youtubeLink)
            throws Throwable {
        final CountDownLatch signal = new CountDownLatch(1);
        YouTubeExtractor.LOGGING = true;

        testUrl = null;

        runTestOnUiThread(new Runnable() {

            @Override
            public void run() {
                final AsyncYouTubeExtractor ytEx = new AsyncYouTubeExtractor(getInstrumentation()
                        .getTargetContext()) {
                    @Override
                    public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
                        assertNotNull(ytFiles);
                        int numNotDash = 0;
                        int itag;
                        for (int i = 0; i < ytFiles.size(); i++) {
                            itag = ytFiles.keyAt(i);
                            if (ytFiles.get(itag).getFormat().isDashContainer()) {
                                numNotDash = i;
                                break;
                            }
                        }
                        itag = ytFiles.keyAt(new Random().nextInt(ytFiles.size() - numNotDash) + numNotDash);
                        Log.d(EXTRACTOR_TEST_TAG, "Testing itag:" + itag);
                        testUrl = ytFiles.get(itag).getUrl();
                        signal.countDown();
                    }
                };
                ytEx.extract(youtubeLink, true, true);
            }
        });

        signal.await(10, TimeUnit.SECONDS);

        assertNotNull(testUrl);

        final URL url = new URL(testUrl);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int code = con.getResponseCode();
        assertEquals(code, 200);
        con.disconnect();
    }


    private void extractorTest(final String youtubeLink, final VideoMeta expMeta)
            throws Throwable {
        final CountDownLatch signal = new CountDownLatch(1);
        YouTubeExtractor.LOGGING = true;

        testUrl = null;

        runTestOnUiThread(new Runnable() {

            @Override
            public void run() {
                final AsyncYouTubeExtractor ytEx = new AsyncYouTubeExtractor(getInstrumentation()
                        .getTargetContext()) {
                    @Override
                    public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
                        assertEquals(expMeta.getVideoId(), videoMeta.getVideoId());
                        assertEquals(expMeta.getTitle(), videoMeta.getTitle());
                        assertEquals(expMeta.getAuthor(), videoMeta.getAuthor());
                        assertEquals(expMeta.getChannelId(), videoMeta.getChannelId());
                        assertEquals(expMeta.getVideoLength(), videoMeta.getVideoLength());
                        assertNotSame(0, videoMeta.getViewCount());
                        assertNotNull(ytFiles);
                        int itag = ytFiles.keyAt(new Random().nextInt(ytFiles.size()));
                        Log.d(EXTRACTOR_TEST_TAG, "Testing itag:" + itag);
                        testUrl = ytFiles.get(itag).getUrl();
                        signal.countDown();
                    }
                };
                ytEx.extract(youtubeLink, false, true);
            }
        });

        signal.await(10, TimeUnit.SECONDS);

        assertNotNull(testUrl);

        final URL url = new URL(testUrl);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int code = con.getResponseCode();
        assertEquals(code, 200);
        con.disconnect();
    }

}
