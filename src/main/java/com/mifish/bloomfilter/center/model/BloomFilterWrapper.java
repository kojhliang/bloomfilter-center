package com.mifish.bloomfilter.center.model;

import com.google.common.base.Strings;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.hash.Hashing;
import com.mifish.bloomfilter.center.util.BloomFilterUtils;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.mifish.bloomfilter.center.BloomFilterConstant.DEFAULT_CHARSET_GBK;

/**
 * Description:
 * <p>
 * BloomFilterWrapper
 * <p>
 * <p>
 *
 * @author : rls
 * Date: 2017-10-13 20:47
 */
public class BloomFilterWrapper implements Serializable {

    private static final long serialVersionUID = 8335173601564112736L;

    /*** bloomfilters*/
    private List<BloomFilter<CharSequence>> bloomfilters = null;

    /***charsetName*/
    private String charsetName = DEFAULT_CHARSET_GBK;

    /***charset*/
    private transient volatile Charset charset = null;

    /***timeVersion*/
    private Date timeVersion = null;

    /***falsePositiveProbability*/
    private double falsePositiveProbability;

    /***expectedNumberOfElements*/
    private long expectedNumberOfElements;

    /***numberOfBits*/
    private long numberOfBits = 0L;

    /***numberOfAddedElements*/
    private volatile long numberOfAddedElements = 0;

    /***布隆索引文件大小，单位：字节数*/
    private long bloomfilterFileSize = 0L;

    /**
     * BloomFilterWrapper
     *
     * @param falsePositiveProbability
     * @param expectedNumberOfElements
     */
    public BloomFilterWrapper(double falsePositiveProbability, long expectedNumberOfElements) {
        this(falsePositiveProbability, expectedNumberOfElements, DEFAULT_CHARSET_GBK, new Date());
    }

    /**
     * BloomFilterWrapper
     *
     * @param falsePositiveProbability
     * @param expectedNumberOfElements
     * @param charsetName
     * @param timeVersion
     */
    public BloomFilterWrapper(double falsePositiveProbability, long expectedNumberOfElements, String charsetName,
                              Date timeVersion) {
        checkArgument(falsePositiveProbability >= 0, "falsePositiveProbability in bloomfilter must bigger than zero.");
        checkArgument(expectedNumberOfElements >= 0, "expectedNumberOfElements in bloomfilter must bigger than zero.");
        checkArgument(Strings.isNullOrEmpty(charsetName), "charsetName in bloomfilter cannot be empty.");
        this.falsePositiveProbability = falsePositiveProbability;
        this.expectedNumberOfElements = expectedNumberOfElements;
        this.numberOfBits = BloomFilterUtils.caculateOptimalNumOfBits(expectedNumberOfElements,
                falsePositiveProbability);
        this.charsetName = charsetName;
        this.timeVersion = timeVersion;
        initCharset();
        initBloomfilters(falsePositiveProbability, expectedNumberOfElements);
    }

    /**
     * initBloomfilters
     *
     * @param falsePositiveProbability
     * @param expectedNumberOfElements
     */
    private void initBloomfilters(double falsePositiveProbability, long expectedNumberOfElements) {
        double singleBfMaxAddElements = BloomFilterUtils.caculateNumberAddElements(Integer.MAX_VALUE,
                falsePositiveProbability);
        double singleBfElements = expectedNumberOfElements;
        int bflen = 1;
        //假如比整形最大值，还需要大，分桶设计
        if (expectedNumberOfElements > Integer.MAX_VALUE) {
            bflen = (int) Math.ceil(expectedNumberOfElements % singleBfMaxAddElements);
            singleBfElements = Math.round(expectedNumberOfElements / bflen) + 1;
        }
        this.bloomfilters = new ArrayList<>(bflen);
        checkArgument(bflen < 1, "the length of bloomfilters cannot be smaller than one.");
        for (int i = 0; i < bflen; i++) {
            BloomFilter<CharSequence> bf = BloomFilter.create(Funnels.stringFunnel(this.charset), (long)
                    singleBfElements, falsePositiveProbability);
            this.bloomfilters.add(bf);
        }
    }


    /***注意多线程*/
    private void initCharset() {
        if (this.charset == null) {
            synchronized (this) {
                if (this.charset == null) {
                    this.charset = Charset.forName(this.charsetName);
                }
            }
        }
    }

    /**
     * getBloomfilterBucketLength
     *
     * @return
     */
    public int getBloomfilterBucketLength() {
        return this.bloomfilters.size();
    }

    /**
     * add
     *
     * @param element
     * @return
     */
    public boolean add(CharSequence element) {
        if (element == null) {
            return false;
        }
        initCharset();
        BloomFilter<CharSequence> bf = this.bloomfilters.get(0);
        if (getBloomfilterBucketLength() > 1) {
            byte[] datas = element.toString().getBytes(this.charset);
            int bfIndex = Math.abs(Hashing.murmur3_128().hashBytes(datas).asInt()) % getBloomfilterBucketLength();
            bf = this.bloomfilters.get(bfIndex);
        }
        synchronized (bf) {
            bf.put(element);
            numberOfAddedElements++;
            return true;
        }
    }

    /**
     * addAll
     *
     * @param elements
     */
    public void addAll(Collection<? extends CharSequence> elements) {
        if (elements == null || elements.isEmpty()) {
            return;
        }
        for (CharSequence element : elements) {
            add(element);
        }
    }

    /**
     * Returns true if the element could have been inserted into the Bloom filter.
     * Use getFalsePositiveProbability() to calculate the probability of this
     * being correct.
     *
     * @param element element to check.
     * @return true if the element could have been inserted into the Bloom filter.
     */
    public boolean contains(CharSequence element) {
        if (element == null) {
            return false;
        }
        initCharset();
        BloomFilter<CharSequence> bf = this.bloomfilters.get(0);
        if (getBloomfilterBucketLength() > 1) {
            byte[] datas = element.toString().getBytes(this.charset);
            int bfIndex = Math.abs(Hashing.murmur3_128().hashBytes(datas).asInt()) % getBloomfilterBucketLength();
            bf = this.bloomfilters.get(bfIndex);
        }
        return bf.mightContain(element);
    }

    /**
     * Getter method for property <tt>timeVersion</tt>
     *
     * @return property value of timeVersion
     */
    public Date getTimeVersion() {
        return timeVersion;
    }

    /**
     * setTimeVersion
     *
     * @param timeVersion
     */
    public void setTimeVersion(Date timeVersion) {
        this.timeVersion = timeVersion;
    }

    /**
     * getCharsetName
     *
     * @return
     */
    public String getCharsetName() {
        return this.charset.name();
    }

    /**
     * setCharsetName
     *
     * @param charsetName
     */
    public void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }

    /**
     * getFalsePositiveProbability
     *
     * @return
     */
    public double getFalsePositiveProbability() {
        return falsePositiveProbability;
    }

    /**
     * getExpectedNumberOfElements
     *
     * @return
     */
    public long getExpectedNumberOfElements() {
        return expectedNumberOfElements;
    }

    /**
     * getNumberOfAddedElements
     *
     * @return
     */
    public long getNumberOfAddedElements() {
        return numberOfAddedElements;
    }

    /**
     * getNumberOfBits
     *
     * @return
     */
    public long getNumberOfBits() {
        return numberOfBits;
    }

    /**
     * getBloomfilterFileSize
     *
     * @return
     */
    public long getBloomfilterFileSize() {
        return bloomfilterFileSize;
    }

    /**
     * setBloomfilterFileSize
     *
     * @param bloomfilterFileSize
     */
    public void setBloomfilterFileSize(long bloomfilterFileSize) {
        this.bloomfilterFileSize = bloomfilterFileSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BloomFilterWrapper that = (BloomFilterWrapper) o;

        if (Double.compare(that.falsePositiveProbability, falsePositiveProbability) != 0) {
            return false;
        }
        if (expectedNumberOfElements != that.expectedNumberOfElements) {
            return false;
        }
        if (numberOfAddedElements != that.numberOfAddedElements) {
            return false;
        }
        if (bloomfilters != null ? !bloomfilters.equals(that.bloomfilters) : that.bloomfilters != null) {
            return false;
        }
        if (charsetName != null ? !charsetName.equals(that.charsetName) : that.charsetName != null) {
            return false;
        }
        if (charset != null ? !charset.equals(that.charset) : that.charset != null) {
            return false;
        }
        return timeVersion != null ? timeVersion.equals(that.timeVersion) : that.timeVersion == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = bloomfilters != null ? bloomfilters.hashCode() : 0;
        result = 31 * result + (charsetName != null ? charsetName.hashCode() : 0);
        result = 31 * result + (charset != null ? charset.hashCode() : 0);
        result = 31 * result + (timeVersion != null ? timeVersion.hashCode() : 0);
        temp = Double.doubleToLongBits(falsePositiveProbability);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (expectedNumberOfElements ^ (expectedNumberOfElements >>> 32));
        result = 31 * result + (int) (numberOfAddedElements ^ (numberOfAddedElements >>> 32));
        return result;
    }
}
