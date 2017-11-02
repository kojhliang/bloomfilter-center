package com.mifish.bloomfilter.center.template;

import com.mifish.bloomfilter.center.model.BloomFilterTask;
import com.mifish.bloomfilter.center.model.BloomFilterTaskResult;
import com.mifish.bloomfilter.center.repository.BloomFilterConfigRepository;
import com.mifish.bloomfilter.center.repository.BloomFilterLockRepository;
import com.mifish.bloomfilter.center.repository.BloomFilterOutputRepository;

import java.util.Date;

/**
 * Description:
 *
 * @author: rls
 * Date: 2017-10-14 11:50
 */
public interface BloomFilterLoadTemplate {

    /**
     * load
     *
     * @param task
     * @param startTaskTime
     * @return
     */
    BloomFilterTaskResult load(BloomFilterTask task, Date startTaskTime);

    /**
     * getBloomFilterLockRepository
     *
     * @return
     */
    BloomFilterLockRepository getBloomFilterLockRepository();

    /**
     * getBloomFilterConfigRepository
     *
     * @return
     */
    BloomFilterConfigRepository getBloomFilterConfigRepository();

    /**
     * getBloomFilterOutputRepository
     *
     * @return
     */
    BloomFilterOutputRepository getBloomFilterOutputRepository();
}
