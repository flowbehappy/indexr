package io.indexr.segment.pack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.indexr.segment.PackExtIndex;

public class ExtIndexExpiredMemCache extends ExpiredMemCache implements ExtIndexMemCache {
    private static final Logger logger = LoggerFactory.getLogger(ExtIndexExpiredMemCache.class);

    public ExtIndexExpiredMemCache(long expireMS, long maxCap) {
        this(expireMS, maxCap, 2000, TimeUnit.SECONDS.toMillis(20), 1000, TimeUnit.MINUTES.toMillis(15));
    }

    public ExtIndexExpiredMemCache(long expireMS, long maxCap, int fullCountThreshold, long frequencyThresholdMS, int dirtyThreshold, long minGCPeriodMS) {
        super("pack", expireMS, maxCap, fullCountThreshold, frequencyThresholdMS, dirtyThreshold, minGCPeriodMS);
        logger.info("Expire = {} minutes", expireMS / 1000 / 60);
        logger.info("Capacity = {} MB", maxCap / 1024 / 1024);
    }

    @Override
    public int packCount() {
        return (int) cache.size();
    }

    @Override
    public void putPack(long segmentId, int columnId, int packId, PackExtIndex pack) {
        cache.put(MemCache.genKey(segmentId, columnId, packId), new ItemWrapper(pack));
    }

    @Override
    public void removePack(long segmentId, int columnId, int packId) {
        cache.invalidate(MemCache.genKey(segmentId, columnId, packId));
    }

    @Override
    public PackExtIndex getPack(long segmentId, int columnId, int packId) {
        ItemWrapper iw;
        return (iw = cache.getIfPresent(MemCache.genKey(segmentId, columnId, packId))) == null ? null : iw.get();
    }

    @Override
    public PackExtIndex getPack(long segmentId, int columnId, int packId, Callable<PackExtIndex> loader) {
        try {
            ItemWrapper iw = cache.get(MemCache.genKey(segmentId, columnId, packId), () -> new ItemWrapper(loader.call()));
            return iw == null ? null : iw.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}

