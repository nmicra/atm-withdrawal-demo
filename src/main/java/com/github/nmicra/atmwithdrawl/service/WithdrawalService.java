package com.github.nmicra.atmwithdrawl.service;

import com.github.nmicra.atmwithdrawl.pojo.WithdrawalRestrictionState;
import com.github.nmicra.atmwithdrawl.util.FileUtils;
import com.google.common.util.concurrent.Striped;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

@Service
public class WithdrawalService {
    private ConcurrentHashMap<String, WithdrawalRestrictionState> withdrawalRestrictionMap = new ConcurrentHashMap<>();

    @Autowired
    private Environment environment;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int STRIPES = 10;
    private static final int TRY_LOCK_NUM = 100;
    private static final int TRY_LOCK_DELAY = 20;
    private static final Striped<Lock> lockStripes = Striped.lazyWeakLock(STRIPES);


    @Value("${withdrawal.restriction.times}")
    private Integer restrictionTimes;

    @Value("${withdrawal.restriction.amount}")
    private Double restrictionAmount;


    @PostConstruct
    protected void init() throws IOException, ClassNotFoundException {
        logger.info(">>> Configured restrictionTimes : %s , restrictionAmount : %s".formatted(restrictionTimes,restrictionAmount));
        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(prof -> prof.equalsIgnoreCase("test"))) {
            return;
        }
        Path path = Path.of("mymap.ser");
        if (Files.exists(path)) {
            logger.info(">>> Found recovery file - mymap.ser");
            var deserializedMap = FileUtils.deserializeConcurrentHashMap("mymap.ser");
            withdrawalRestrictionMap.putAll(deserializedMap);
            Files.deleteIfExists(path);
        }
    }

    public Pair<Boolean, String> withdrawalAllowed(String cardId, Double amount) {
        logger.debug(">>> WithdrawalRequested cardId [%s] , amount [%s]".formatted(cardId,amount));
        if (amount > restrictionAmount) return new ImmutablePair(false, "amount %s is too big".formatted(amount));

        var key = cardId + ":" + LocalDate.now();
        Lock lock = lockStripes.get(key);
        for (int i = 0; i < TRY_LOCK_NUM; i++) {
            if (lock.tryLock()) {
                try {
                    if (!withdrawalRestrictionMap.containsKey(key)) {
                        WithdrawalRestrictionState updatedRestrictionState = new WithdrawalRestrictionState(restrictionTimes - 1, restrictionAmount - amount, amount);
                        withdrawalRestrictionMap.put(key, updatedRestrictionState);
                        return new ImmutablePair(true, updatedRestrictionState.toString());
                    } else {
                        var restrictions = withdrawalRestrictionMap.get(key);
                        if (restrictions.allowedTimes() <= 0) return new ImmutablePair(false, "try again tomorrow. Allowed withdrawal times per day %s".formatted(restrictionTimes));
                        if (restrictions.amountLimit() < amount) return new ImmutablePair(false, "you are exceeding the allowed amount per day which is %s".formatted(restrictionAmount));

                        WithdrawalRestrictionState updatedRestrictionState = new WithdrawalRestrictionState(restrictions.allowedTimes() - 1, restrictions.amountLimit() - amount, amount);
                        withdrawalRestrictionMap.put(key, updatedRestrictionState);
                        return new ImmutablePair(true, updatedRestrictionState.toString());
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                try{Thread.sleep(TRY_LOCK_DELAY);} catch (Throwable tw){}
            }
        }
        throw new IllegalStateException("couldn't acquire lock");

    }

    public WithdrawalRestrictionState getStateForCardId(String cardId) {
        var key = cardId + ":" + LocalDate.now();
        return withdrawalRestrictionMap.get(key);
    }


    public Pair<Boolean, String> cancelWithdrawal(String cardId) {

        var key = cardId + ":" + LocalDate.now();
        if (!withdrawalRestrictionMap.containsKey(key)) {
            return new ImmutablePair(false, "you haven't performed any withdrawals to cancel it");
        }

        var restrictions = withdrawalRestrictionMap.get(key);
        if (restrictions.lastCancelableAmount() == null){
            return new ImmutablePair(false, "it's possible to cancel only the last withdrawal");
        }

        Lock lock = lockStripes.get(key);
        for (int i = 0; i < TRY_LOCK_NUM; i++) {
            if (lock.tryLock()) {
                try {
                    WithdrawalRestrictionState withdrawalRestrictionState = new WithdrawalRestrictionState(restrictions.allowedTimes() + 1, restrictions.amountLimit() + restrictions.lastCancelableAmount(), null);
                    withdrawalRestrictionMap.put(key, withdrawalRestrictionState);
                    return new ImmutablePair(true, withdrawalRestrictionState.toString());
                } finally {
                    lock.unlock();
                }
            } else {
                try{Thread.sleep(TRY_LOCK_DELAY);} catch (Throwable tw){}
            }
        }

        throw new IllegalStateException("couldn't acquire lock");

    }


    @PreDestroy
    private void preDestroyed() throws IOException {
        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(prof -> prof.equalsIgnoreCase("test"))) {
            return;
        }
        logger.info(">>> Terminating serializing the withdrawalRestrictionMap into mymap.ser");
        FileUtils.serializeConcurrentHashMap(withdrawalRestrictionMap, "mymap.ser");
        logger.info(">>> Terminating serialization done");
    }

}


