package org.broadinstitute.hellbender.tools.sv.cluster;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import org.broadinstitute.hellbender.tools.sv.SVCallRecord;
import org.broadinstitute.hellbender.tools.sv.SVCallRecordUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.variant.VariantContextGetters;

import java.util.List;
import java.util.Set;

import static org.broadinstitute.hellbender.tools.spark.sv.utils.GATKSVVCFConstants.COPY_NUMBER_FORMAT;

/**
 * Clustering engine class for defragmenting depth-based DEL/DUP calls, such as those produced by
 * {@link org.broadinstitute.hellbender.tools.copynumber.GermlineCNVCaller GermlineCNVCaller}. Each variant is first padded by a fraction
 * of its length and then merged with any other overlapping variants that meet the minimum sample overlap. Additionally,
 * singleton variants (with only one carrier sample) will only be merged with variants of the same copy number.
 */
public class CNVDefragmenter extends SVClusterEngine<SVCallRecord> {

    public static final double DEFAULT_SAMPLE_OVERLAP = 0.8;
    public static final double DEFAULT_PADDING_FRACTION = 0.25;

    protected final double minSampleOverlap;
    protected final double paddingFraction;

    public CNVDefragmenter(final SAMSequenceDictionary dictionary, final double paddingFraction,
                           final double minSampleOverlap) {
        super(dictionary, CLUSTERING_TYPE.SINGLE_LINKAGE, false,
                (new SVCollapser(SVCollapser.BreakpointSummaryStrategy.MIN_START_MAX_END))::collapse);
        this.minSampleOverlap = minSampleOverlap;
        this.paddingFraction = paddingFraction;
    }

    @Override
    boolean clusterTogether(final SVCallRecord a, final SVCallRecord b) {
        // Only do clustering on depth-only CNVs
        if (!a.isDepthOnly() || !b.isDepthOnly()) return false;
        if (!a.isCNV() || !b.isCNV()) return false;
        Utils.validate(a.getContigA().equals(a.getContigB()), "Variant A is a CNV but interchromosomal");
        Utils.validate(b.getContigA().equals(b.getContigB()), "Variant B is a CNV but interchromosomal");

        // Types match
        if (a.getType() != b.getType()) return false;

        // Interval overlap
        if (!getPaddedRecordInterval(a.getContigA(), a.getPositionA(), a.getPositionB())
                .overlaps(getPaddedRecordInterval(b.getContigA(), b.getPositionA(), b.getPositionB()))) return false;

        // Sample overlap
        if (!hasSampleOverlap(a, b, minSampleOverlap)) {
            return false;
        }

        // In the single-sample case, match copy number strictly if we're looking at the same sample
        // TODO repeated check for CN attributes in hasSampleOverlap and getCarrierSamples
        final Set<String> carriersA = getCarrierSamples(a);
        final Set<String> carriersB = getCarrierSamples(b);
        if (carriersA.size() == 1 && carriersA.equals(carriersB)) {
            final Genotype genotypeA = a.getGenotypes().get(carriersA.iterator().next());
            final Genotype genotypeB = b.getGenotypes().get(carriersB.iterator().next());
            if (genotypeA.hasExtendedAttribute(COPY_NUMBER_FORMAT) && genotypeB.hasExtendedAttribute(COPY_NUMBER_FORMAT)) {
                final int copyNumberA = VariantContextGetters.getAttributeAsInt(genotypeA, COPY_NUMBER_FORMAT, 0);
                final int copyNumberB = VariantContextGetters.getAttributeAsInt(genotypeB, COPY_NUMBER_FORMAT, 0);
                final int copyNumberDeltaA = genotypeA.getPloidy() - copyNumberA;
                final int copyNumberDeltaB = genotypeB.getPloidy() - copyNumberB;
                if (copyNumberDeltaA != copyNumberDeltaB) {
                    return false;
                }
            } else {
                final List<Allele> sortedAllelesA = SVCallRecordUtils.sortAlleles(genotypeA.getAlleles());
                final List<Allele> sortedAllelesB = SVCallRecordUtils.sortAlleles(genotypeB.getAlleles());
                if (!sortedAllelesA.equals(sortedAllelesB)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Max clusterable position depends on the other variant's length, which is unknown, so we assume the largest
     * possible event that would extend to the end of the contig.
     */
    @Override
    protected int getMaxClusterableStartingPosition(final SVCallRecord call) {
        final int contigLength = dictionary.getSequence(call.getContigA()).getSequenceLength();
        final int maxTheoreticalStart = (int) Math.floor((call.getPositionB() + paddingFraction * (call.getLength() + contigLength)) / (1.0 + paddingFraction));
        return Math.min(maxTheoreticalStart, dictionary.getSequence(call.getContigA()).getSequenceLength());
    }

    /**
     * Determine an overlap interval for clustering using specified padding.
     * @return padded interval
     */
    protected SimpleInterval getPaddedRecordInterval(final String contig, final int start, final int end) {
        return new SimpleInterval(contig, start, end)
                .expandWithinContig((int) (paddingFraction * (end - start + 1)), dictionary);
    }
}
