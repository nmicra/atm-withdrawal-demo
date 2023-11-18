package com.github.nmicra.atmwithdrawl;

import com.github.nmicra.atmwithdrawl.pojo.WithdrawalRestrictionState;
import com.github.nmicra.atmwithdrawl.service.WithdrawalService;
import com.google.common.base.Supplier;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.IntStream;

@SpringBootTest(properties = {"withdrawal.restriction.times=200", "logging.level.com.github.nmicra=DEBUG"})
@ActiveProfiles("test")
class AtmWithdrawalConcurrencyTests {

	@Autowired
	private WithdrawalService withdrawalService;

	@Test
	@DisplayName("Concurrency test for withdrawal")
	void testSimpleAmount() {
		IntStream.range(0, 200).parallel().mapToObj(
				n -> {
					Supplier<Pair<Boolean, String>> pairSupplier = n % 2 == 0 ? () -> withdrawalService.withdrawalAllowed("123a", 20.0) : () -> withdrawalService.withdrawalAllowed("123b", 30.0);
					return pairSupplier;
				}
		).forEach(supplier -> supplier.get());

		try {Thread.sleep(200);} catch (InterruptedException e) {throw new RuntimeException(e);}

		WithdrawalRestrictionState stateForCardId123A = withdrawalService.getStateForCardId("123a");
		WithdrawalRestrictionState stateForCardId123B = withdrawalService.getStateForCardId("123b");
		Assertions.assertEquals( 20000 - 20*100, stateForCardId123A.amountLimit());
		Assertions.assertEquals( 100, stateForCardId123A.allowedTimes());
		Assertions.assertEquals(20000 - 30*100, stateForCardId123B.amountLimit());
		Assertions.assertEquals( 100, stateForCardId123B.allowedTimes());

	}

}
