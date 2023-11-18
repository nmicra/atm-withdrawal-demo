package com.github.nmicra.atmwithdrawl.pojo;

import java.io.Serializable;
import java.util.Optional;

public record WithdrawalRestrictionState(Integer allowedTimes, Double amountLimit,
                                         Double lastCancelableAmount) implements Serializable {
}
