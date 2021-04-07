package org.broadinstitute.hellbender.tools.variantdb;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.*;
import org.apache.commons.lang.StringUtils;
import org.broadinstitute.hellbender.tools.variantdb.arrays.RawArrayTsvCreator;
import org.broadinstitute.hellbender.utils.genotyper.IndexedAlleleList;
import org.broadinstitute.hellbender.utils.variant.GATKVCFConstants;
import org.broadinstitute.hellbender.utils.variant.GATKVCFHeaderLines;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

//TODO rename this or get rid of it. a place holder for now
public class CommonCode {
    public static String CALL_RATE = "CALL_RATE";
    public static String INVARIANT = "INVARIANT";
    public static String HWE = "HWE";

    public static String HWE_FILTER = "HWE";
    public static String CALL_RATE_FILTER = "CALL_RATE";
    public static String INVARIANT_FILTER = "INVARIANT";


    /**
     * If the alleles are "missing", -1 will be returned as the index
     * @param variant
     * @return
     */
    public static List<Integer> getGTAlleleIndexes(final VariantContext variant) {
        IndexedAlleleList<Allele> alleleList = new IndexedAlleleList<>(variant.getAlleles());
        ArrayList<Integer> allele_indices = new ArrayList<Integer>();

        for (Allele allele : variant.getGenotype(0).getAlleles()) {
            allele_indices.add(alleleList.indexOfAllele(allele));
        }
        return allele_indices;
    }

    public static String getGTString(final VariantContext variant) {
        List<Integer> allele_indices = getGTAlleleIndexes(variant);
        if (allele_indices.size() != 2){
            throw new IllegalArgumentException("GT doesnt have two alleles");
        }
        List<String> gsStrings = allele_indices.stream().map(index -> index == -1 ? "." : index.toString()).collect(Collectors.toList());
        String separator = variant.getGenotype(0).isPhased() ? VCFConstants.PHASED : VCFConstants.UNPHASED;
        return StringUtils.join(gsStrings, separator);
    }

    public static VCFHeader generateRawArrayVcfHeader(Set<String> sampleNames, final SAMSequenceDictionary sequenceDictionary) {
        final Set<VCFHeaderLine> lines = new HashSet<>();

        lines.add(VCFStandardHeaderLines.getFormatLine(VCFConstants.GENOTYPE_KEY));
        lines.add(new VCFFormatHeaderLine(RawArrayTsvCreator.NORMX, 1, VCFHeaderLineType.Float, "Normalized X intensity"));
        lines.add(new VCFFormatHeaderLine(RawArrayTsvCreator.NORMY, 1, VCFHeaderLineType.Float, "Normalized Y intensity"));
        lines.add(new VCFFormatHeaderLine(RawArrayTsvCreator.BAF, 1, VCFHeaderLineType.Float, "B Allele Frequency"));
        lines.add(new VCFFormatHeaderLine(RawArrayTsvCreator.LRR, 1, VCFHeaderLineType.Float, "Log R Ratio"));

        // TODO: do we need to differentiate these from the same values calculated on this VCF (ie not the full set)
        lines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.EXCESS_HET_KEY));

        // TODO: are there offical headers for this?
        lines.add(new VCFInfoHeaderLine(CALL_RATE, 1, VCFHeaderLineType.Float, "Call Rate"));
        lines.add(new VCFInfoHeaderLine(INVARIANT, 1, VCFHeaderLineType.Flag, "Invariant"));
        lines.add(new VCFInfoHeaderLine(HWE, 1, VCFHeaderLineType.Float, "Phred-scaled HWE p-value"));

        lines.add(new VCFFilterHeaderLine(INVARIANT_FILTER, "No variant samples in reference QC panel"));
        lines.add(new VCFFilterHeaderLine(CALL_RATE_FILTER, "Inadequate call rate in reference QC panel"));
        lines.add(new VCFFilterHeaderLine(HWE_FILTER, "HWE is violated in reference QC panel"));

        final VCFHeader header = new VCFHeader(lines, sampleNames);
        header.setSequenceDictionary(sequenceDictionary);

        return header;
    }

    public static VCFHeader generateVcfHeader(Set<String> sampleNames,//) { //final Set<VCFHeaderLine> defaultHeaderLines,
                                        final SAMSequenceDictionary sequenceDictionary) {
        final Set<VCFHeaderLine> headerLines = new HashSet<>();

        headerLines.addAll( getEvoquerVcfHeaderLines() );
//        headerLines.addAll( defaultHeaderLines );

        final VCFHeader header = new VCFHeader(headerLines, sampleNames);
        header.setSequenceDictionary(sequenceDictionary);

        return header;
    }


    // TODO is this specific for cohort extract? if so name it such
    public static Set<VCFHeaderLine> getEvoquerVcfHeaderLines() {
        final Set<VCFHeaderLine> headerLines = new HashSet<>();

        // TODO: Get a list of all possible values here so that we can make sure they're in the VCF Header!

        // Add standard VCF fields first:
        VCFStandardHeaderLines.addStandardInfoLines( headerLines, true,
                VCFConstants.STRAND_BIAS_KEY,
                VCFConstants.DEPTH_KEY,
                VCFConstants.RMS_MAPPING_QUALITY_KEY,
                VCFConstants.ALLELE_COUNT_KEY,
                VCFConstants.ALLELE_FREQUENCY_KEY,
                VCFConstants.ALLELE_NUMBER_KEY,
                VCFConstants.END_KEY
        );

        VCFStandardHeaderLines.addStandardFormatLines(headerLines, true,
                VCFConstants.GENOTYPE_KEY,
                VCFConstants.GENOTYPE_QUALITY_KEY,
                VCFConstants.DEPTH_KEY,
                VCFConstants.GENOTYPE_PL_KEY,
                VCFConstants.GENOTYPE_ALLELE_DEPTHS
        );

        headerLines.add(GATKVCFHeaderLines.getFormatLine(GATKVCFConstants.REFERENCE_GENOTYPE_QUALITY));

        headerLines.add(GATKVCFHeaderLines.getFormatLine(GATKVCFConstants.HAPLOTYPE_CALLER_PHASING_GT_KEY));
        headerLines.add(GATKVCFHeaderLines.getFormatLine(GATKVCFConstants.HAPLOTYPE_CALLER_PHASING_ID_KEY));

        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_RAW_RMS_MAPPING_QUALITY_KEY));
        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_RMS_MAPPING_QUALITY_KEY));

        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_RAW_MAP_QUAL_RANK_SUM_KEY));
        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_MAP_QUAL_RANK_SUM_KEY));

        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_RAW_QUAL_APPROX_KEY));
        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.RAW_QUAL_APPROX_KEY));

        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_RAW_READ_POS_RANK_SUM_KEY));
        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_READ_POS_RANK_SUM_KEY));

        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_SB_TABLE_KEY));
        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.SB_TABLE_KEY));

        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_VQS_LOD_KEY));
        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_YNG_STATUS_KEY));

        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_VARIANT_DEPTH_KEY));
        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.VARIANT_DEPTH_KEY));

        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_STRAND_ODDS_RATIO_KEY));

        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_FISHER_STRAND_KEY));

        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.AS_QUAL_BY_DEPTH_KEY));
        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.QUAL_BY_DEPTH_KEY));

        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.EXCESS_HET_KEY));

        headerLines.add(GATKVCFHeaderLines.getInfoLine(GATKVCFConstants.SB_TABLE_KEY));

        headerLines.add(GATKVCFHeaderLines.getFilterLine(GATKVCFConstants.VQSR_TRANCHE_SNP));
        headerLines.add(GATKVCFHeaderLines.getFilterLine(GATKVCFConstants.VQSR_TRANCHE_INDEL));
        headerLines.add(GATKVCFHeaderLines.getFilterLine(GATKVCFConstants.NAY_FROM_YNG));
        headerLines.add(GATKVCFHeaderLines.getFilterLine(GATKVCFConstants.EXCESS_HET_KEY));

        return headerLines;
    }

    public enum ModeEnum {
        EXOMES,
        GENOMES,
        ARRAYS
    }

    public enum OutputType {
        TSV,
        ORC,
        AVRO,
        PARQUET,
        TSV2
    }
}
