package org.broadinstitute.hellbender.tools.funcotator;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.testutils.ArgumentsBuilder;
import org.broadinstitute.hellbender.tools.funcotator.dataSources.DataSourceUtils;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.broadinstitute.hellbender.tools.funcotator.FuncotatorDataSourceBundlerUtils;
import org.broadinstitute.hellbender.tools.funcotator.FuncotatorDataSourceBundler;

/**
 * Class to test the {@link FuncotatorDataSourceBundler} class.
 * Created by Hailey on 8/2/21.
 */

public class FuncotatorDataSourceBundlerIntegrationTest extends CommandLineProgramTest {

    //==================================================================================================================
    // Private Static Members:

    // Off by default because each test case takes ~1 hour to run:
    private static final boolean doFullScaleTests = false;

    //==================================================================================================================
    // Helper Methods:

    private String getDataSourceRemoteURL(final String dsTypeArg) {
        switch (dsTypeArg) {
            case FuncotatorDataSourceBundler.BACTERIA_ARG_LONG_NAME:
                return FuncotatorDataSourceBundler.BACTERIA_BASE_URL;
            case FuncotatorDataSourceBundler.FUNGI_ARG_LONG_NAME:
                return FuncotatorDataSourceBundler.FUNGI_BASE_URL;
            case FuncotatorDataSourceBundler.METAZOA_ARG_LONG_NAME:
                return FuncotatorDataSourceBundler.METAZOA_BASE_URL;
            case FuncotatorDataSourceBundler.PLANTS_ARG_LONG_NAME:
                return FuncotatorDataSourceBundler.PLANTS_BASE_URL;
            case FuncotatorDataSourceBundler.PROTISTS_ARG_LONG_NAME:
                return FuncotatorDataSourceBundler.PROTISTS_BASE_URL;
            default: throw new GATKException("Data source type does not exist: " + dsTypeArg);
        }
    }


    private void verifyDataSourcesExistThenDeleteThem(final String dsOrgArg, final String dsSpeciesArg, final boolean doExtract) {
        // Get the path to our files:
        final Path currentPath          = IOUtils.getPath(".");
        final Path remoteDataSourcePath = IOUtils.getPath(getDataSourceRemoteURL(dsOrgArg) + "/" + FuncotatorDataSourceBundlerUtils.getDSFileName(dsOrgArg, dsSpeciesArg) + DataSourceUtils.GTF_GZ_EXTENSION);
        final Path expectedDownloadedDataSourcePath = currentPath.resolve(remoteDataSourcePath.getFileName().toString());

        // Verify it exists and delete it:
        verifyDataSourcesExistThenDeleteThem(expectedDownloadedDataSourcePath, doExtract);
    }

    private void verifyDataSourcesExistThenDeleteThem(final Path expectedDownloadedDataSourcePath, final boolean doExtract) {

        // Make sure our file exists:
        Assert.assertTrue( Files.exists(expectedDownloadedDataSourcePath) );

        // Clean up the downloaded files:
        try {
            Files.delete(expectedDownloadedDataSourcePath);
            if ( doExtract ) {
                // Get the base name for our folder.
                // (this way we get rid of all extensions (i.e. both `tar` and `gz`):
                final String baseName = expectedDownloadedDataSourcePath.toFile().getName().replace(".gtf.gz", "");
                final Path   extractedDataSourceFolder = expectedDownloadedDataSourcePath.resolveSibling(baseName);
                FileUtils.deleteDirectory(extractedDataSourceFolder.toFile());
            }
        }
        catch ( final IOException ex ) {
            throw new GATKException("Could not clean up downloaded data sources for testDownloadRealDataSources: " + expectedDownloadedDataSourcePath);
        }
    }

    //==================================================================================================================
    // Data Providers:

    @DataProvider
    private Object[][] provideForTestDownload() {
        return new Object[][]{
                {
                    FuncotatorDataSourceBundler.METAZOA_ARG_LONG_NAME,
                        "acyrthosiphon_pisum",
                        false,
                        true

                }
//                {
//                        FuncotatorDataSourceBundler.BACTERIA_ARG_LONG_NAME,
//                        "absiella_dolichum_dsm_3991_gca_000154285/",
//                        true,
//                        true
//                }
//                {
//                        FuncotatorDataSourceBundler.BACTERIA_ARG_LONG_NAME,
//                        "species-name acinebactor_baumannii_aye",
//                        true
//                },
//                {
//                        FuncotatorDataSourceBundler.BACTERIA_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        false
//                },
//                {
//                        FuncotatorDataSourceBundler.FUNGI_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        true
//                },
//                {
//                        FuncotatorDataSourceBundler.FUNGI_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        true
//                },
//                {
//                        FuncotatorDataSourceBundler.FUNGI_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        false
//                },
//                {
//                        FuncotatorDataSourceBundler.METAZOA_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        true
//                },
//                {
//                        FuncotatorDataSourceBundler.METAZOA_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        true
//                },
//                {
//                        FuncotatorDataSourceBundler.METAZOA_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        false
//                },
//                {
//                        FuncotatorDataSourceBundler.PLANTS_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        true
//                },
//                {
//                        FuncotatorDataSourceBundler.PLANTS_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        true
//                },
//                {
//                        FuncotatorDataSourceBundler.PLANTS_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        false
//                },
//                {
//                        FuncotatorDataSourceBundler.PROTISTS_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        true
//                },
//                {
//                        FuncotatorDataSourceBundler.PROTISTS_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        true
//                },
//                {
//                        FuncotatorDataSourceBundler.PROTISTS_ARG_LONG_NAME,
//                        "acinebactor_baumannii_aye",
//                        false
//                }
        };
    }

    //==================================================================================================================
    // Tests:

    @Test( dataProvider = "provideForTestDownload")
    void testDownloadRealDataSources (final String dsOrgArg, final String dsSpeciesArg, final boolean doOverwrite, final boolean doExtract) {
        final ArgumentsBuilder arguments = new ArgumentsBuilder();

        //File outputFile = new File("Absiella_dolichum_dsm_3991_gca_000154285.ASM15428v1.51.gtf");
        arguments.add(dsOrgArg, true);
        arguments.add("species-name", dsSpeciesArg);
        //arguments.add("output", outputFile);
        arguments.add(FuncotatorDataSourceBundler.OVERWRITE_ARG_LONG_NAME, doOverwrite);
        arguments.add(FuncotatorDataSourceBundler.EXTRACT_AFTER_DOWNLOAD, doExtract);

        runCommandLine(arguments);

        // Now verify we got the data sources and clean up the files
        // so we don't have up to 30 gigs of stuff lying around:
//        verifyDataSourcesExistThenDeleteThem(dsOrgArg, dsSpeciesArg, doExtract);
    }

}
