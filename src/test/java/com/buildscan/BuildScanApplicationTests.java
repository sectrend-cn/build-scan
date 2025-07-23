/*
package com.sectrend.buildscan;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class BuildScanApplicationTests {

    @Test
    public void wfpForFile_correct_for_try_c() throws IOException, InterruptedException {

        //String actualWFP = Winnowing.wfpForFile("Winnowing.java", "src/main/java/com/sectrend/buildscan/buildTools/scanner/Winnowing.java").trim();
        //String expectedWFP = FileUtils.readFileToString(new File("D:src/test/resources/Winnowing.java-scan.wfp"), Charset.defaultCharset());
    */
/*    //assertEquals(expectedWFP, actualWFP);

        String overrideAPIURL = System.getenv("SCANOSS_API_URL");
        String overrideAPIKEY = System.getenv("SCANOSS_API_KEY");
        ScannerConf conf = ScannerConf.defaultConf();
        if (StringUtils.isNotEmpty(overrideAPIURL)) {
            conf = new ScannerConf(overrideAPIURL, overrideAPIKEY);
        }
        Scanner scanner = new Scanner(conf);

        String dir = "C:\\Users\\TUF\\Desktop\\blackduck\\gradle\\gradle-build-quickstart";
        ScanType scanType=null;
        String sbomPath="";
        ScanFormat format=null;
        String outfile="C:\\Users\\TUF\\Desktop\\blackduck\\gradle\\output";

        scanner.scanDirectory(dir,scanType,sbomPath,format,outfile,"");*//*


    }


//
//    @Test
//    public void wfpForEmptyfile() throws IOException {
//        String actualWFP1 = Winnowing.wfpForFile("empty.java", "src/test/resources/empty.java").trim();
//        String actualWFP2 = Winnowing.wfpForFile("empty.java", "src/test/resources/empty2.java").trim();
//
//        System.out.printf("Empty output: %s\n", actualWFP1 );
//        System.out.printf("Empty output: %s\n", actualWFP2 );
//    }
//
//    @Test
//    public void scanFile_try_c_plain() throws Exception{
//        Scanner scanner = new Scanner(ScannerConf.defaultConf());
//        scanner.scanFileAndSave("src/main/java/com/sectrend/buildscan/buildTools/scanner/Winnowing.java", null, "", null, "");
//    }
//
//    @Test
//    public void scanFile_try_empty() throws Exception{
//        Scanner scanner = new Scanner(ScannerConf.defaultConf());
//        scanner.scanFileAndSave("src/test/resources/empty.java", null, "", null, "");
//    }

}
*/
